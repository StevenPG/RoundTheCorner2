import com.android.build.api.dsl.ApplicationExtension
import com.stevenpg.roundthecorner2.buildlogic.configureKotlinAndroid
import com.stevenpg.roundthecorner2.buildlogic.libs
import com.stevenpg.roundthecorner2.buildlogic.pluginId
import com.stevenpg.roundthecorner2.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Conventions for the single `:app` module. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-application"))

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = libs.version("targetSdk").toInt()
        }
    }
}
