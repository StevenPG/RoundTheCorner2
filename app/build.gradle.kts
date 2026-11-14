plugins {
    alias(libs.plugins.rtc.android.application)
    alias(libs.plugins.rtc.android.compose)
    alias(libs.plugins.rtc.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.stevenpg.roundthecorner2"

    defaultConfig {
        applicationId = "com.stevenpg.roundthecorner2"
        // The release workflow passes -Prtc.versionName=<tag without the v>; local builds use the default.
        val releaseVersion = providers.gradleProperty("rtc.versionName").orNull
        versionName = releaseVersion ?: "0.1.0"
        versionCode = releaseVersion?.let(::versionCodeFor) ?: 1
    }

    /*
     * Release signing. Android only installs an update signed with the same key
     * as the installed app, so published APKs must always use one fixed key
     * (like a code-signing certificate). CI provides it through environment
     * variables from GitHub secrets (see tools/setup_release_signing.sh).
     * Without them, release builds fall back to the debug key so they still
     * install locally; the release workflow refuses to publish those.
     */
    val releaseKeystore = providers.environmentVariable("RTC_KEYSTORE_FILE").orNull
    if (releaseKeystore != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("RTC_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("RTC_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("RTC_KEY_PASSWORD").get()
        }
    }

    buildFeatures {
        buildConfig = true
    }

    /*
     * Distribution flavors (see README "Build flavors"):
     *
     *  full - sideloaded APK. Will hold the SEND_SMS permission and send SMS
     *         automatically in the background.
     *  play - Google Play build. Play policy forbids SEND_SMS for non-default
     *         SMS apps, so SMS triggers instead raise a "tap to send" prompt.
     *
     * Only the SMS sender differs; everything else is shared code. The
     * flavor-specific SMS implementations land in the messaging milestone.
     */
    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            // Distinct id so both builds can be installed side by side.
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
            buildConfigField("boolean", "AUTOMATIC_SMS", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "AUTOMATIC_SMS", "false")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    /*
     * MapLibre ships native code for every CPU architecture (~12 MB each).
     * Play builds are uploaded as an App Bundle, which Play splits per device
     * automatically. For sideloaded APKs we build one APK per architecture so
     * a phone only downloads what it can run (arm64-v8a covers modern phones;
     * x86_64 is for emulators). AGP can't split while bundling, so splits are
     * switched off for `bundle*` tasks.
     */
    val buildingBundle = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }
    splits {
        abi {
            isEnable = !buildingBundle
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:messaging"))
    implementation(project(":core:monitoring"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(project(":feature:triggers"))
    implementation(project(":feature:places"))
    implementation(project(":feature:recipients"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.turbine)
}

/**
 * Turns "1.2.3" into 10203, so every release gets a larger versionCode than the
 * one before (Android won't install an update with a lower one). Allows up to
 * 99 minor and 99 patch versions.
 */
fun versionCodeFor(versionName: String): Int {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(versionName)
        ?: error("Release version must look like 1.2.3, got '$versionName'")
    val (major, minor, patch) = match.destructured.toList().map(String::toInt)
    require(minor < 100 && patch < 100) { "minor and patch must be below 100: $versionName" }
    return major * 10_000 + minor * 100 + patch
}
