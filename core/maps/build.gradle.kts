/*
 * MapLibre (open-source map renderer) wrapped for Compose. Isolated in its own
 * module so the native map library is a dependency of exactly one place, and
 * screens can swap in a placeholder for JVM screenshot tests.
 */
plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.android.compose)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.maps"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:domain")) // Geodesy (radius circle).
    implementation(project(":core:designsystem"))
    implementation(libs.maplibre.android)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
