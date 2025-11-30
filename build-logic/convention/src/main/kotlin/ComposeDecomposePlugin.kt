import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import utils.versionCatalog

class ComposeDecomposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val decomposeCatalog = versionCatalog("decompose")
            extensions.configure<KotlinMultiplatformExtension>("kotlin") {
                configureDecomposeSourceSets(decomposeCatalog)
            }
        }
    }

    private fun KotlinMultiplatformExtension.configureDecomposeSourceSets(catalog: VersionCatalog) {
        sourceSets {
            commonMain.dependencies {
                implementation(catalog.library("decompose"))
                implementation(catalog.library("decompose.compose"))
                implementation(catalog.library("decompose.instancekeeper"))
                implementation(catalog.library("decompose.statekeeper"))
                implementation(catalog.library("decompose.lifecycle"))
            }
        }
    }
}