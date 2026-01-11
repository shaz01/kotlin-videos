package com.olcayaras.vidster.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.olcayaras.vidster.di.initKoin
import com.olcayaras.vidster.ui.navigation.NavigationContent
import com.olcayaras.vidster.ui.theme.AppTheme

@Composable
internal fun App(
    componentContext: ComponentContext = DefaultComponentContext(LifecycleRegistry())
) = AppTheme {
    initKoin()

    val rootViewModel = RootViewModel(componentContext)

    NavigationContent(
        stack = rootViewModel.childStack,
        navigationFactory = Route,
        backHandler = rootViewModel.backHandler,
        onBack = rootViewModel::onBackClicked
    )
}
