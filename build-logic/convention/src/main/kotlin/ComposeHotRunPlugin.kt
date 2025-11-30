import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

/**
 * IMPORTANT: add this to your build Gradle.
 * tasks.register<ComposeHotRun>("runHot") {
 *      mainClass.set("MainKt")
 * }
 */
class ComposeHotRunPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configurePlugins()
            configureComposeCompiler()
        }
    }

    private fun Project.configurePlugins() {
        pluginManager.apply(catalog.pluginId("hotRun"))
    }

    private fun Project.configureComposeCompiler() {
        //https://github.com/JetBrains/compose-hot-reload
        extensions.configure<ComposeCompilerGradlePluginExtension>("composeCompiler") {
            featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
        }
    }
}