plugins {
    alias(libs.plugins.rtc.android.feature)
}

android {
    namespace = "com.stevenpg.roundthecorner2.feature.triggers"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:maps"))
    implementation(project(":core:monitoring"))
    implementation(libs.androidx.activity.compose) // Permission launchers.
}
