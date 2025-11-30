package com.olcayaras.vidster.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import com.olcayaras.vidster.ViewModel
import com.olcayaras.vidster.ui.navigation.NavigationFactory
import com.olcayaras.vidster.ui.screens.detail.DetailScreen
import com.olcayaras.vidster.ui.screens.detail.DetailViewModel
import com.olcayaras.vidster.ui.screens.home.HomeScreen
import com.olcayaras.vidster.ui.screens.home.HomeViewModel

/**
 * Base routes for the app.
 */
@Serializable
sealed class Route {
    @Serializable
    data object Home : Route()

    @Serializable
    data class Detail(val name: String) : Route()


    companion object : NavigationFactory<Route> {
        override val initialRoute: Route = Home
        override val kSerializer: KSerializer<Route> get() = serializer()

        override fun createChild(
            route: Route,
            componentContext: ComponentContext,
            navigation: StackNavigation<Route>,
        ): ViewModel<*, *> {
            return when (route) {
                Home -> HomeViewModel(componentContext, navigation)
                is Detail -> DetailViewModel(componentContext, navigation, route.name)
            }
        }

        @Composable
        override fun childContent(viewModel: ViewModel<*,*>) {
            when (viewModel) {
                is HomeViewModel -> {
                    val state by viewModel.models.collectAsState()
                    HomeScreen(state, viewModel::take)
                }
                is DetailViewModel -> {
                    val state by viewModel.models.collectAsState()
                    DetailScreen(state, viewModel::take)
                }

                else -> throw IllegalStateException("Instance is $viewModel but that class is not known")
            }
        }
    }
}