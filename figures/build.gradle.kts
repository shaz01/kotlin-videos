import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("convention.android.library")
    id("convention.kmp.base")
    id("convention.kmp.serialization")
    id("convention.compose.base")
    id("convention.compose.decompose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":videoscript-previewer"))
        }
    }
}

android.namespace = "$ProjectId.figures"