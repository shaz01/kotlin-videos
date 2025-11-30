package com.olcayaras.vidster.di

import io.ktor.client.HttpClient
import com.olcayaras.vidster.network.DetailApi
import com.olcayaras.vidster.network.provideHttpClient
import org.koin.dsl.module

val ApiModule = module {
    single<HttpClient> { provideHttpClient() }

    single<DetailApi> { DetailApi(get()) }
}