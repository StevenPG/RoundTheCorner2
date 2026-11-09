plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.android.compose)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.designsystem"
}

dependencies {
    // Exposed as `api` so every feature gets the same Material + icon set.
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
}
