plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.network"
}

dependencies {
    api(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.okhttp.mockwebserver)
}
