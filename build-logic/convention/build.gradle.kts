plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpBase") {
            id = "convention.kmp.base"
            implementationClass = "KmpBasePlugin"
        }
        register("kmpSerialization") {
            id = "convention.kmp.serialization"
            implementationClass = "KmpSerializationPlugin"
        }
        register("composeBase") {
            id = "convention.compose.base"
            implementationClass = "ComposeBasePlugin"
        }
        register("composeDecompose") {
            id = "convention.compose.decompose"
            implementationClass = "ComposeDecomposePlugin"
        }
        register("androidPlugin") {
            id = "convention.android.library"
            implementationClass = "AndroidLibraryPlugin"
        }
        register("publishPlugin") {
            id= "convention.publish"
            implementationClass = "PublishPlugin"
        }
    }
}