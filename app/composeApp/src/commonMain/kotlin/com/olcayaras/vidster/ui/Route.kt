@file:OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)

package com.olcayaras.vidster.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntSize
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.vidster.ViewModel
import com.olcayaras.vidster.previewer.rememberVideoController
import com.olcayaras.vidster.ui.navigation.NavigationFactory
import com.olcayaras.vidster.ui.screens.editor.EditorScreen
import com.olcayaras.vidster.ui.screens.editor.EditorViewModel
import com.olcayaras.vidster.ui.screens.video.VideoEvent
import com.olcayaras.vidster.ui.screens.video.VideoScreen
import com.olcayaras.vidster.ui.screens.video.VideoViewModel

/**
 * Base routes for the app.
 */
@Serializable
sealed class Route {
    @Serializable
    data object Editor : Route()

    @Serializable
    data object Video : Route()

    companion object : NavigationFactory<Route> {
        // Shared state for passing frames between screens (not serialized in route)
        private var pendingVideoFrames: List<SegmentFrame> = emptyList()
        private var pendingVideoScreenSize: IntSize = IntSize(1920, 1080)

        override val initialRoute: Route = Editor
        override val kSerializer: KSerializer<Route> get() = serializer()

        fun navigateToVideo(
            navigation: StackNavigation<Route>,
            frames: List<SegmentFrame>,
            screenSize: IntSize
        ) {
            pendingVideoFrames = frames
            pendingVideoScreenSize = screenSize
            navigation.push(Video)
        }

        override fun createChild(
            route: Route,
            componentContext: ComponentContext,
            navigation: StackNavigation<Route>,
        ): ViewModel<*, *> {
            return when (route) {
                is Editor -> EditorViewModel(
                    c = componentContext,
                    onPlayAnimation = { frames, screenSize ->
                        navigateToVideo(navigation, frames, screenSize)
                    }
                )
                is Video -> VideoViewModel(
                    c = componentContext,
                    frames = pendingVideoFrames,
                    screenSize = pendingVideoScreenSize,
                    onExit = { navigation.pop() }
                )
            }
        }

        @Composable
        override fun childContent(viewModel: ViewModel<*, *>) {
            when (viewModel) {
                is EditorViewModel -> {
                    val state by viewModel.models.collectAsState()
                    EditorScreen(state, viewModel::take)
                }

                is VideoViewModel -> {
                    val state by viewModel.models.collectAsState()
                    state.animation?.let { animation ->
                        val controller = rememberVideoController(animation.duration, fps = 60)
                        VideoScreen(
                            animation = animation,
                            videoResolution = state.screenSize,
                            videoController = controller,
                            onExit = { viewModel.take(VideoEvent.Exit) }
                        )
                    }
                }

                else -> throw IllegalStateException("Instance is $viewModel but that class is not known")
            }
        }
    }
}
