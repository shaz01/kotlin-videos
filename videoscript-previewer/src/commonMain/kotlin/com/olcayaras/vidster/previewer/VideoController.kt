@file:OptIn(ExperimentalTime::class)

package com.olcayaras.vidster.previewer

import androidx.compose.runtime.*
import com.olcayaras.lib.asDuration
import com.olcayaras.lib.ofFrames
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class VideoController(
    val totalDuration: Duration,
    val fps: Int
) {
    val maxFrames = totalDuration.ofFrames(fps)

    var isPlaying by mutableStateOf(false)

    private val _currentFrame = mutableIntStateOf(0)
    val currentFrame: IntState = _currentFrame


    val currentDuration = derivedStateOf { totalDuration * currentFrame.value / max(1, maxFrames) }

    private var wasSeekPerformed = false
    suspend fun updateFrames() {
        val previousFramesAsDuration = currentFrame.value.asDuration(fps)

        val startTime = Clock.System.now().toEpochMilliseconds() - previousFramesAsDuration.inWholeMilliseconds
        while (isPlaying) {
            val currentTime = Clock.System.now().toEpochMilliseconds() - startTime
            val newFrame = ((currentTime / 1000f) * fps).toInt() % maxFrames
            _currentFrame.value = newFrame
            delay(1000L / fps)
        }
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun seekToFrame(frame: Int) {
        _currentFrame.value = frame.coerceIn(0, maxFrames - 1)
        wasSeekPerformed = true
    }

    fun seekToDuration(duration: Duration) {
        val frame = duration.ofFrames(fps)
        seekToFrame(frame)
    }

    fun seekToProgress(progress: Float) {
        val frame = (progress * maxFrames).toInt()
        seekToFrame(frame)
    }

    val progress: Float
        get() = if (maxFrames > 0) currentFrame.value.toFloat() / maxFrames else 0f
}

@Composable
fun rememberVideoController(
    duration: Duration,
    fps: Int = 60
): VideoController {
    return remember(duration, fps) { VideoController(duration, fps) }
}