plugins {
    alias(libs.plugins.kotlinJvm)
    alias(ktorhttp.plugins.plugin)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "org.company.backend"
version = "1.0.0"
application {
    mainClass.set("org.company.backend.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(project(":app:shared"))
    
    implementation(ktorhttp.client.core)
    implementation(ktorhttp.client.cio)
    implementation(ktorhttp.client.content.negotiation)
    implementation(ktorhttp.serialization)
    implementation(ktorhttp.server.core)
    implementation(ktorhttp.server.netty)
    implementation(ktorhttp.server.contentnegotiation)
    implementation(ktorhttp.server.auth)
    implementation(ktorhttp.server.auth.jwt)
    implementation(ktorhttp.server.resources)
//    implementation(ktorhttp.server.routing)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.logback.classic)
    implementation(ktorhttp.server.config.yaml)
    
    testImplementation(ktorhttp.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}