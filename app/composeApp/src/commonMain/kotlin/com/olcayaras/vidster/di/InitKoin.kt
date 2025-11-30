package com.olcayaras.vidster.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Initializes Koin DI with modular architecture support.
 * Explicitly includes provided feature modules plus any additional modules.
 */
private fun initKoin(
    vararg modules: Module,
    extras: (KoinApplication.() -> Unit)? = null,
) {
    startKoin {
        extras?.invoke(this)

        // Include all feature modules
        modules(*modules)
    }
}

fun initKoin() = initKoin(
    // list your modules here.
    ApiModule,
)