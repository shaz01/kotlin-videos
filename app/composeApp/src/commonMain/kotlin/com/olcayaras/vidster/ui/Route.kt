@file:OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)

package com.olcayaras.vidster.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntSize
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.olcayaras.figures.Figure
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.vidster.ViewModel
import com.olcayaras.vidster.previewer.rememberVideoController
import com.olcayaras.vidster.ui.navigation.NavigationFactory
import com.olcayaras.vidster.ui.screens.editor.EditorScreen
import com.olcayaras.vidster.ui.screens.editor.EditorViewModel
import com.olcayaras.vidster.ui.screens.editfigure.EditFigureScreen
import com.olcayaras.vidster.ui.screens.editfigure.EditFigureViewModel
import com.olcayaras.vidster.ui.screens.video.VideoEvent
import com.olcayaras.vidster.ui.screens.video.VideoScreen
import com.olcayaras.vidster.ui.screens.video.VideoViewModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Base routes for the app.
 */
@Serializable
sealed class Route {
    @Serializable
    data object Editor : Route()

    @Serializable
    data class Video(
        val videoFrames: List<SegmentFrame>,
        val videoScreenWidth: Int,
        val videoScreenHeight: Int,
        val fps: Int
    ) : Route()

    @Serializable
    data class EditFigure(
        val figure: Figure?,
        val figureIndex: Int?,
        @Transient
        val onFinish: ((Figure?) -> Unit)? = null,
    ) : Route()

    companion object : NavigationFactory<Route> {
        override val initialRoute: Route = Editor
        override val kSerializer: KSerializer<Route> get() = serializer()

        override fun createChild(
            route: Route,
            componentContext: ComponentContext,
            navigation: StackNavigation<Route>,
        ): ViewModel<*, *> {
            return when (route) {
                is Editor -> EditorViewModel(c = componentContext, navigation = navigation)

                is Video -> VideoViewModel(
                    c = componentContext,
                    frames = route.videoFrames,
                    screenSize = IntSize(route.videoScreenWidth, route.videoScreenHeight),
                    fps = route.fps,
                    onExit = { navigation.pop() }
                )

                is EditFigure -> EditFigureViewModel(
                    c = componentContext,
                    initialFigure = route.figure,
                    onSave = { figure ->
                        route.onFinish?.invoke(figure)
                        navigation.pop()
                    },
                    onCancel = {
                        route.onFinish?.invoke(null)
                        navigation.pop()
                    }
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
                        val controller = rememberVideoController(animation.duration, fps = state.fps)
                        VideoScreen(
                            animation = animation,
                            videoResolution = state.screenSize,
                            videoController = controller,
                            onExit = { viewModel.take(VideoEvent.Exit) }
                        )
                    }
                }

                is EditFigureViewModel -> {
                    val state by viewModel.models.collectAsState()
                    EditFigureScreen(state, viewModel::take)
                }

                else -> throw IllegalStateException("Instance is $viewModel but that class is not known")
            }
        }
    }
}
