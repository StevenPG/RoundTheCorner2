plugins {
    alias(libs.plugins.rtc.android.feature)
}

android {
    namespace = "com.stevenpg.roundthecorner2.feature.recipients"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:messaging"))
    implementation(libs.androidx.activity.compose) // Permission + contact-picker launchers.
}
