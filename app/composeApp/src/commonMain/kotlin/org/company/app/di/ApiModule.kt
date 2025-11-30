package org.company.app.di

import io.ktor.client.HttpClient
import org.company.app.network.DetailApi
import org.company.app.network.provideHttpClient
import org.koin.dsl.module

val ApiModule = module {
    single<HttpClient> { provideHttpClient() }

    single<DetailApi> { DetailApi(get()) }
}