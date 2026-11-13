/*
 * Pure business logic: geodesy, the proximity/hysteresis state machine,
 * adaptive polling, message templating and distance formatting.
 *
 * No Android dependency on purpose - everything here runs (and is tested) on a
 * plain JVM in milliseconds, and could be reused server-side unchanged.
 */
plugins {
    alias(libs.plugins.rtc.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
