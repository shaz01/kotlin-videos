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
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(ktorhttp.client.android)
        }

        jvmMain.dependencies {
            implementation(ktorhttp.client.cio)
        }

        iosMain.dependencies {
            implementation(ktorhttp.client.darwin)
        }
    }
}

android {
    namespace = "$ProjectId.app"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        targetSdk = 36

        applicationId = "$ProjectId.androidApp"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
