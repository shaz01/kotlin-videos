package org.company.backend

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.Resources
import io.ktor.server.routing.Route
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.java.KoinJavaComponent.inject

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(Resources)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    configureSecurity()
    configureFrameworks()
    configureRouting()
}


// Fix for Koin `inject`  for Ktor 3.0.3 using Koin 4.0.4 in Routes (e.g. in Routing.kt)
inline fun <reified T : Any> Route.inject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) =
    lazy {
        GlobalContext.getKoinApplicationOrNull()?.koin?.get<T>(qualifier, parameters) ?:
        inject<T>(T::class.java).value // uses org.koin.java.KoinJavaComponent.inject
    }