package com.stevenpg.roundthecorner2.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Java/Kotlin bytecode level used everywhere. */
internal val JAVA_VERSION = JavaVersion.VERSION_17

/**
 * Settings shared by the app module and every Android library module.
 *
 * Note: AGP 9 compiles Kotlin itself ("built-in Kotlin"), so we no longer
 * apply `org.jetbrains.kotlin.android` - only configure the compiler.
 */
internal fun Project.configureKotlinAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
        // Robolectric/Roborazzi tests need merged Android resources on the JVM.
        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.all { test ->
            // Robolectric pokes at JDK internals (FileDescriptor) that JDK 17+
            // hides behind the module system unless explicitly exported.
            test.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
            )
            test.maxHeapSize = "2g"
        }
    }
    configureKotlinCompiler()
}

/** Shared Kotlin compiler flags for Android and pure-JVM modules. */
internal fun Project.configureKotlinCompiler() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
            // Treat warnings as errors in CI only, so local iteration stays fast.
            allWarningsAsErrors.set(providers.environmentVariable("CI").isPresent)
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }
}
