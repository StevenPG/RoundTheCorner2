/*
 * Shared composables that understand the domain model (unlike
 * core:designsystem, which is model-agnostic). E.g. how a PlaceIcon looks.
 */
plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.android.compose)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
}
