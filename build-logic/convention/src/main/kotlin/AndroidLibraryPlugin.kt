import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryPlugin: Plugin<Project>{
    override fun apply(target: Project) {
        with(target){
            applyAndroidLibraryPlugin(target)

            val androidBlock = extensions.getByType<LibraryExtension>()
            configureAndroidBlock(androidBlock)
        }
    }


    private fun applyAndroidLibraryPlugin(target: Project) {
        with(target) {
            pluginManager.apply(catalog.pluginId("android-library"))
        }
    }

    private fun configureAndroidBlock(android: LibraryExtension){
        with(android){
            compileSdk = AndroidConfig.compileSdk
            defaultConfig {
                minSdk = AndroidConfig.minSdk
                testInstrumentationRunner = AndroidConfig.testInstrumentationRunner
            }
        }
    }
}