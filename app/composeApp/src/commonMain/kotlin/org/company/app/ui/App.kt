package org.company.app.ui.entrypoint

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.company.app.di.initKoin
import org.company.app.ui.navigation.NavigationContent
import org.company.app.ui.Route
import org.company.app.ui.theme.AppTheme

@Composable
internal fun App() = AppTheme {
    initKoin()

    val rootViewModel = RootViewModel(DefaultComponentContext(LifecycleRegistry()))

    NavigationContent(
        stack = rootViewModel.childStack,
        navigationFactory = Route,
        backHandler = rootViewModel.backHandler,
        onBack = rootViewModel::onBackClicked
    )
}
