plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.hilt)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.location"
}

dependencies {
    api(project(":core:domain")) // LocationFix
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
