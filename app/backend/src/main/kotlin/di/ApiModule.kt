package org.company.backend.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.company.backend.network.DetailApi
import org.koin.dsl.module

val ApiModule = module {
    single<HttpClient> { provideHttpClient() }
    single<DetailApi> { DetailApi(get()) }
}

fun provideHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
            ignoreUnknownKeys = true
        })
    }
}