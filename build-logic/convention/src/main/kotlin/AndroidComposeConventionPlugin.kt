import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.stevenpg.roundthecorner2.buildlogic.libs
import com.stevenpg.roundthecorner2.buildlogic.library
import com.stevenpg.roundthecorner2.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose on an Android module (app or library) and adds the
 * baseline Compose dependencies plus the Roborazzi screenshot-test stack.
 *
 * Apply *after* `rtc.android.application` or `rtc.android.library`.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-compose"))
        pluginManager.apply(libs.pluginId("roborazzi"))

        val android = extensions.findByType(ApplicationExtension::class.java)
            ?: extensions.getByType(LibraryExtension::class.java)
        (android as CommonExtension).buildFeatures.compose = true

        dependencies {
            val bom = libs.library("androidx-compose-bom")
            add("implementation", platform(bom))
            add("testImplementation", platform(bom))

            add("implementation", libs.library("androidx-compose-ui"))
            add("implementation", libs.library("androidx-compose-ui-graphics"))
            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))

            // JVM screenshot tests (Robolectric renders, Roborazzi captures PNGs).
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("roborazzi-junit-rule"))
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}
