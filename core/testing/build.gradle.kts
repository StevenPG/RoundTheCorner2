/*
 * Shared test utilities (fakes, screenshot helpers). Only ever added as a
 * `testImplementation` dependency - never shipped in the app.
 */
plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.android.compose)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.testing"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:data"))
    api(project(":core:messaging"))
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(libs.roborazzi.junit.rule)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
}
