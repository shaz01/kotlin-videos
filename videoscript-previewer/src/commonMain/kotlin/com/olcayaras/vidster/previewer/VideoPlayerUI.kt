package com.olcayaras.vidster.previewer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.olcayaras.lib.AudioController
import com.olcayaras.lib.definitions.SequencesAsVideo
import com.olcayaras.lib.definitions.VideoDefinition
import com.olcayaras.vidster.previewer.previewers.VideoPlayer
import kotlin.time.DurationUnit

@Composable
fun VideoPlayerUI(
    screenSize: IntSize,
    contentScale: Float = 1f,
    backgroundColor: Color = Color.White,
    fps: Int = 60,
    video: VideoDefinition,
) {
    val length = video.duration
    Column(Modifier.fillMaxHeight().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val videoController = rememberVideoController(length, fps)
        val audioController = remember { AudioController(video.audioDefinitions) }

        val currentDuration by videoController.currentDuration
        val currentFrame by videoController.currentFrame
        LaunchedEffect(currentDuration) {
            audioController.updateTime(currentDuration)
        }

        // gets fps through videoController
        VideoPlayer(
            modifier = Modifier.fillMaxWidth().border(2.dp, Color.Black),
            screenSize = screenSize,
            contentScale = contentScale,
            controller = videoController,
            background = backgroundColor,
            content = {
                SequencesAsVideo(video.sequenceDefinitions)
            },
        )

        Slider(
            modifier = Modifier.Companion,
            value = currentFrame.toFloat() / videoController.maxFrames.toFloat(),
            onValueChange = { videoController.seekToProgress(it) }
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                """${videoController.currentFrame}/${videoController.maxFrames}
                            |${
                    currentDuration.toString(
                        DurationUnit.SECONDS,
                        decimals = 3
                    )
                }/${videoController.totalDuration}
                        """.trimMargin(),
                modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(
                onClick = {
                    videoController.togglePlayPause()
                    val isPlaying = videoController.isPlaying
                    if (isPlaying) {
                        audioController.play()
                    } else {
                        audioController.pause()
                    }
                },
                content = {
                    val icon = if (videoController.isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    }
                    Icon(icon, null)
                }
            )
        }
    }
}
