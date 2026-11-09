/*
 * Delivers a rendered message to a recipient. One MessageSender per channel
 * (Slack, Discord, SMS); MessageDispatcher routes by channel.
 */
plugins {
    alias(libs.plugins.rtc.android.library)
    alias(libs.plugins.rtc.hilt)
}

android {
    namespace = "com.stevenpg.roundthecorner2.core.messaging"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:network")) // Shared OkHttpClient.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
