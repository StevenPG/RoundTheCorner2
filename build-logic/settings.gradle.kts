/*
 * build-logic is an *included build* holding our Gradle convention plugins.
 * It lets every module apply one short plugin id (e.g. `rtc.android.feature`)
 * instead of copy/pasting the same 40 lines of Android config - the Gradle
 * equivalent of a Maven parent POM, but composable.
 */
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "build-logic"
include(":convention")
