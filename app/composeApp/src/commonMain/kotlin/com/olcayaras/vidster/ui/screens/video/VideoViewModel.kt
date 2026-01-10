package com.olcayaras.vidster.ui.screens.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.arkivanov.decompose.ComponentContext
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.lib.definitions.VideoDefinition
import com.olcayaras.vidster.ViewModel
import com.olcayaras.vidster.utils.buildAnimation
import kotlinx.coroutines.flow.Flow

sealed interface VideoEvent {
    data object Exit : VideoEvent
}

data class VideoState(
    val animation: VideoDefinition?,
    val screenSize: IntSize,
    val fps: Int,
    val isLoading: Boolean = true
)

class VideoViewModel(
    c: ComponentContext,
    private val frames: List<SegmentFrame>,
    private val screenSize: IntSize,
    private val fps: Int,
    private val onExit: () -> Unit
) : ViewModel<VideoEvent, VideoState>(c) {

    private var animation by mutableStateOf<VideoDefinition?>(null)
    private var isLoading by mutableStateOf(true)

    @Composable
    override fun models(events: Flow<VideoEvent>): VideoState {
        LaunchedEffect(Unit) {
            animation = buildAnimation(frames, screenSize, fps)
            isLoading = false
        }

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is VideoEvent.Exit -> onExit()
                }
            }
        }

        return VideoState(
            animation = animation,
            screenSize = screenSize,
            fps = fps,
            isLoading = isLoading
        )
    }
}
