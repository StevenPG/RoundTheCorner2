import com.android.build.api.dsl.LibraryExtension
import com.stevenpg.roundthecorner2.buildlogic.configureKotlinAndroid
import com.stevenpg.roundthecorner2.buildlogic.libs
import com.stevenpg.roundthecorner2.buildlogic.library
import com.stevenpg.roundthecorner2.buildlogic.pluginId
import com.stevenpg.roundthecorner2.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Conventions for every Android library module (core:* and feature:*). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // Lint/consumer rules default to the module's own targetSdk.
            lint.targetSdk = libs.version("targetSdk").toInt()
        }

        dependencies {
            add("testImplementation", libs.library("junit4"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("turbine"))
        }
    }
}
