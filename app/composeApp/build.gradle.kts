import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("com.android.application")
    id("convention.kmp.base")
    id("convention.kmp.serialization")
    id("convention.compose.base")
    id("convention.compose.decompose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(ktorhttp.client.core)
            implementation(ktorhttp.client.content.negotiation)
            implementation(ktorhttp.serialization)
            implementation(libs.molecule)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
            implementation(project(":figures"))
            implementation(project(":videoscript-previewer"))
            implementation(project(":videoscript-core"))
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(ktorhttp.client.android)
        }

        jvmMain.dependencies {
            implementation(ktorhttp.client.cio)
            implementation(project(":videoscript-rendering"))
        }

        iosMain.dependencies {
            implementation(ktorhttp.client.darwin)
        }
    }
}

android {
    namespace = "$ProjectId.app"
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        minSdk = AndroidConfig.minSdk
        targetSdk = AndroidConfig.compileSdk

        applicationId = "$ProjectId.androidApp"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = AndroidConfig.testInstrumentationRunner
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Multiplatform App"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "$ProjectId.desktopApp"
            }
        }
    }
}
