plugins {
    id("convention.android.library")
    id("convention.kmp.base")
    id("convention.kmp.serialization")
    id("convention.compose.base")
    id("convention.compose.decompose")
}
val lwjglVersion = "3.3.3"

android {
    namespace = "$ProjectId.videoscript.core"
}

kotlin {
    jvm()
    js { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io)

            implementation(project.dependencies.platform("org.kotlincrypto.hash:bom:0.7.0"))
            implementation("org.kotlincrypto.hash:sha2")
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)

            implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
            implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")

            implementation(project.dependencies.platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
            implementation("org.lwjgl:lwjgl")
            implementation("org.lwjgl:lwjgl-openal")
            implementation("org.lwjgl:lwjgl-stb")

            val lwjglNatives = when (org.gradle.internal.os.OperatingSystem.current()) {
                org.gradle.internal.os.OperatingSystem.LINUX -> "natives-linux"
                org.gradle.internal.os.OperatingSystem.MAC_OS -> "natives-macos-arm64"
                org.gradle.internal.os.OperatingSystem.WINDOWS -> "natives-windows"
                else -> throw Error("Unsupported OS")
            }

            runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")

            implementation("org.bytedeco:javacv-platform:1.5.11")
        }
    }
}

//https://github.com/JetBrains/compose-hot-reload
//composeCompiler {
//    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
//}
