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
        versionCode = 1
        versionName = "0.1.0"
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
            // Signing config is supplied by CI / local keystore later; until then
            // release builds are signed with the debug key so they can be installed.
            signingConfig = signingConfigs.getByName("debug")
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
    testImplementation(libs.turbine)
}
