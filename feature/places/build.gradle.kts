plugins {
    alias(libs.plugins.rtc.android.feature)
}

android {
    namespace = "com.stevenpg.roundthecorner2.feature.places"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:maps"))
}
