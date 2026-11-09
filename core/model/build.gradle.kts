plugins {
    alias(libs.plugins.rtc.jvm.library)
}

dependencies {
    // Flow lives in coroutines-core, which is pure Kotlin (no Android).
    api(libs.kotlinx.coroutines.core)
}
