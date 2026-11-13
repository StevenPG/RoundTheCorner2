plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.hilt)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.runtime)
}
