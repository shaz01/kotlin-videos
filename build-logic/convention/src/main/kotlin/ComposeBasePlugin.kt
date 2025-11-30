import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.compose.ComposePlugin.Dependencies

class ComposeBasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configurePlugins()

            extensions.configure<KotlinMultiplatformExtension>("kotlin") {
                configureSourceSets(catalog)
            }
        }
    }

    private fun Project.configurePlugins() {
        pluginManager.apply(catalog.pluginId("compose-compiler"))
        pluginManager.apply(catalog.pluginId("compose"))
    }

    private fun KotlinMultiplatformExtension.configureSourceSets(libs: VersionCatalog) {
        val compose = extensions.getByName("compose") as Dependencies

        sourceSets {
            commonMain.dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.library("composeIcons-featherIcons"))
            }

            commonTest.dependencies {
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }

            androidMain.dependencies {
                implementation(compose.uiTooling)
                implementation(libs.library("androidx-activityCompose"))
            }

            jvmMain.dependencies {
                implementation(compose.desktop.currentOs)
            }

            jsMain.dependencies {
                implementation(compose.html.core)
            }
        }
    }
}