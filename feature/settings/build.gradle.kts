plugins {
    alias(libs.plugins.rtc.android.feature)
}

android {
    namespace = "com.stevenpg.roundthecorner2.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
}
