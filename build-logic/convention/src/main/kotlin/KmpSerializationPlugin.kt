import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpSerializationPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configurePlugin()

            val kt = extensions.findByType<KotlinMultiplatformExtension>()!!
            configureSourceSets(kt, catalog)
        }
    }

    private fun Project.configurePlugin() {
        pluginManager.apply(catalog.pluginId("kotlinx-serialization"))
    }

    private fun configureSourceSets(kt: KotlinMultiplatformExtension, catalog: VersionCatalog) {
        with(kt) {
            sourceSets {
                commonMain.dependencies {
                    implementation(catalog.library("kotlinx-serialization"))
                    implementation(catalog.library("kotlinx-serialization-json"))
                }
            }
        }
    }
}
