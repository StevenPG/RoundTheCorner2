plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.database"
}

room {
    // Each schema version is exported as JSON and committed, so migrations can
    // be verified against the exact shape users have on their devices.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
}
