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
import com.olcayaras.vidster.ui.screens.editor.EditorScreen
import com.olcayaras.vidster.ui.screens.editor.EditorViewModel

/**
 * Base routes for the app.
 */
@Serializable
sealed class Route {
    @Serializable
    data object Editor : Route()


    companion object : NavigationFactory<Route> {
        override val initialRoute: Route = Editor
        override val kSerializer: KSerializer<Route> get() = serializer()

        override fun createChild(
            route: Route,
            componentContext: ComponentContext,
            navigation: StackNavigation<Route>,
        ): ViewModel<*, *> {
            return when (route) {
                is Editor -> EditorViewModel(componentContext)
            }
        }

        @Composable
        override fun childContent(viewModel: ViewModel<*, *>) {
            when (viewModel) {
                is EditorViewModel -> {
                    val state by viewModel.models.collectAsState()
                    EditorScreen(state, viewModel::take)
                }

                else -> throw IllegalStateException("Instance is $viewModel but that class is not known")
            }
        }
    }
}