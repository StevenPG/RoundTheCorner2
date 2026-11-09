import com.stevenpg.roundthecorner2.buildlogic.JAVA_VERSION
import com.stevenpg.roundthecorner2.buildlogic.configureKotlinCompiler
import com.stevenpg.roundthecorner2.buildlogic.libs
import com.stevenpg.roundthecorner2.buildlogic.library
import com.stevenpg.roundthecorner2.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Pure Kotlin/JVM module with no Android dependency (e.g. `core:model`).
 * Keeping the domain Android-free makes it trivially unit-testable and fast
 * to compile.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-jvm"))

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        configureKotlinCompiler()

        dependencies {
            add("testImplementation", libs.library("junit4"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
        }
    }
}
