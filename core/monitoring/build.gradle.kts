/*
 * The always-on part of the app: a location foreground service that runs the
 * MonitoringEngine, shows the live progress notification, fires triggers,
 * retries failed sends with WorkManager, and restarts after reboot.
 */
plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.hilt)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.monitoring"
}

dependencies {
    api(project(":core:data"))
    implementation(project(":core:location"))
    implementation(project(":core:messaging"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
}
