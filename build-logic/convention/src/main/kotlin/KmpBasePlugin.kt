import KmpConfig.Companion.getKmpConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

private data class KmpConfig(
    val enableJvmTarget: Boolean,
    val enableAndroidTarget: Boolean,
    val enableDevTargetOptimization: Boolean,
    val enableIosTargets: Boolean,
    val enableJsTarget: Boolean,
    val enableWasmTargets: Boolean,
    val jvmToolchain: Int,
    val nativeBaseName: String
) {
    companion object {
        fun Project.getKmpConfig(): KmpConfig {
            return KmpConfig(
                enableJvmTarget = findProperty("kmp.enable.jvm")?.toString()?.toBoolean() ?: true,
                enableAndroidTarget = findProperty("kmp.enable.android")?.toString()?.toBoolean() ?: true,
                enableDevTargetOptimization = findProperty("kmp.enable.dev.target.optimization")?.toString()?.toBoolean() ?: false,
                enableIosTargets = findProperty("kmp.enable.ios")?.toString()?.toBoolean() ?: true,
                enableJsTarget = findProperty("kmp.enable.js")?.toString()?.toBoolean() ?: true,
                enableWasmTargets = findProperty("kmp.enable.wasm")?.toString()?.toBoolean() ?: true,
                jvmToolchain = findProperty("kmp.jvm.toolchain")?.toString()?.toIntOrNull() ?: 11,
                nativeBaseName = findProperty("kmp.base.name")?.toString() ?: "TemplateApp",
            )
        }
    }
}

class KmpBasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configurePlugins()

            val config = getKmpConfig()
            extensions.configure<KotlinMultiplatformExtension>("kotlin") {
                jvmToolchain(config.jvmToolchain)
                configureTargets(config)
                configureNativeFramework(config.nativeBaseName)
                applyDefaultHierarchyTemplate()
                configureSourceSets(catalog)
            }
        }
    }


    private fun Project.configurePlugins(){
        pluginManager.apply(catalog.pluginId("multiplatform"))
//        pluginManager.apply(libs.pluginId("kotlinx-serialization"))
    }

    private fun KotlinMultiplatformExtension.configureTargets(config: KmpConfig) {
        if (config.enableJvmTarget) {
            jvm()
        }

        if (config.enableAndroidTarget) {
            androidTarget()
        }

        if (config.enableIosTargets) {
            if (config.enableDevTargetOptimization) {
                val hostArch = System.getProperty("os.arch")
                when {
                    hostArch.contains("aarch64") -> iosSimulatorArm64()
                    else -> iosX64()
                }
            } else {
                iosArm64()
                iosX64()
                iosSimulatorArm64()
            }
        }

        if (config.enableJsTarget) {
            js {
                browser()
                binaries.executable()
            }
        }

        if (config.enableWasmTargets) {
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
                binaries.executable()
            }
        }
    }

    private fun KotlinMultiplatformExtension.configureNativeFramework(nativeBaseName: String) {
        targets.filter { it.name.startsWith("ios") }.forEach {
            val nativeTarget = it as? KotlinNativeTarget ?: return

            nativeTarget.binaries.framework {
                baseName = nativeBaseName
                isStatic = true
            }
        }
    }

    private fun KotlinMultiplatformExtension.configureSourceSets(catalog: VersionCatalog) {
        sourceSets {
            commonMain.dependencies {
                implementation(catalog.library("kotlinx-coroutines-core"))
                implementation(catalog.library("napier"))
            }
            commonTest.dependencies {
                implementation(kotlin("test"))
                implementation(catalog.library("kotlinx-coroutines-test"))
            }
            androidMain.dependencies {
                implementation(catalog.library("kotlinx-coroutines-android"))
            }
            jvmMain.dependencies {
                implementation(catalog.library("kotlinx-coroutines-swing"))
            }
        }
    }
}