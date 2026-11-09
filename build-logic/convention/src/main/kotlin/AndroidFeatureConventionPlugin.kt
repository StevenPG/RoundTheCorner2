import com.stevenpg.roundthecorner2.buildlogic.libs
import com.stevenpg.roundthecorner2.buildlogic.library
import com.stevenpg.roundthecorner2.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * One-stop plugin for `feature:*` modules: Android library + Compose + Hilt,
 * plus the dependencies every screen needs (design system, domain model,
 * ViewModel/lifecycle integration, Navigation 3 keys).
 *
 * Feature modules must never depend on each other; the app module wires them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("rtc-android-library"))
        pluginManager.apply(libs.pluginId("rtc-android-compose"))
        pluginManager.apply(libs.pluginId("rtc-hilt"))
        pluginManager.apply(libs.pluginId("kotlin-serialization"))

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:model"))

            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation3-runtime"))
            add("implementation", libs.library("kotlinx-serialization-json"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
