package com.olcayaras.vidster.ui.screens.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olcayaras.figures.getMockSegmentFrame
import com.olcayaras.lib.definitions.SequencesAsVideo
import com.olcayaras.lib.definitions.VideoDefinition
import com.olcayaras.vidster.previewer.VideoController
import com.olcayaras.vidster.previewer.previewers.VideoPlayer
import com.olcayaras.vidster.previewer.rememberVideoController
import com.olcayaras.vidster.ui.theme.AppTheme
import com.olcayaras.vidster.utils.buildAnimation
import compose.icons.FeatherIcons
import compose.icons.feathericons.Pause
import compose.icons.feathericons.Play
import compose.icons.feathericons.X
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.DurationUnit

private val scrimColor = Color.Black.copy(alpha = 0.6f)
private val transparentColor = Color.Transparent

@Composable
fun VideoScreen(
    modifier: Modifier = Modifier,
    animation: VideoDefinition,
    videoResolution: IntSize,
    backgroundColor: Color = Color.White,
    videoController: VideoController,
    onExit: () -> Unit = {},
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTrigger by remember { mutableIntStateOf(0) }

    // Auto-hide controls after 3 seconds of inactivity
    LaunchedEffect(interactionTrigger, videoController.isPlaying) {
        if (videoController.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    val currentFrame by videoController.currentFrame
    val currentDuration by videoController.currentDuration
    val aspectRatio = remember { videoResolution.width.toFloat() / videoResolution.height.toFloat() }
    var parentAspectRatio by remember { mutableFloatStateOf(-1f) }

    VideoScreenLayout(
        modifier = modifier.onGloballyPositioned {
            parentAspectRatio = it.size.width.toFloat() / it.size.height.toFloat()
        },
        controlsVisible = controlsVisible,
        isPlaying = videoController.isPlaying,
        progress = currentFrame.toFloat() / videoController.maxFrames.toFloat(),
        currentTimeSeconds = currentDuration.inWholeSeconds,
        totalTimeSeconds = videoController.totalDuration.toLong(DurationUnit.SECONDS),
        onPlayPauseClick = {
            videoController.togglePlayPause()
            interactionTrigger++
        },
        onSeek = { progress ->
            videoController.seekToProgress(progress)
            interactionTrigger++
        },
        onExit = onExit,
        onToggleControls = {
            controlsVisible = !controlsVisible
            interactionTrigger++
        }
    ) {
        VideoPlayer(
            modifier = Modifier
                .align(Alignment.Center)
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = parentAspectRatio > aspectRatio),
            screenSize = videoResolution,
            contentScale = 1f,
            background = backgroundColor,
            controller = videoController,
        ) {
            SequencesAsVideo(animation.sequenceDefinitions)
        }
    }
}

@Composable
private fun VideoScreenLayout(
    modifier: Modifier = Modifier,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    progress: Float,
    currentTimeSeconds: Long,
    totalTimeSeconds: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onExit: () -> Unit,
    onToggleControls: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleControls
            )
    ) {
        content()

        // Top gradient overlay with exit button
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(scrimColor, transparentColor)
                        )
                    )
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = FeatherIcons.X,
                        contentDescription = "Exit",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Bottom gradient overlay with controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(transparentColor, scrimColor)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onPlayPauseClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) FeatherIcons.Pause else FeatherIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Progress slider
                    Slider(
                        value = progress,
                        onValueChange = onSeek,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    // Time display
                    Text(
                        text = formatDuration(currentTimeSeconds) + " / " + formatDuration(totalTimeSeconds),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

@Preview
@Composable
private fun VideoScreenPreview() {
    AppTheme {
        VideoScreenLayout(
            controlsVisible = true,
            isPlaying = false,
            progress = 0.3f,
            currentTimeSeconds = 12,
            totalTimeSeconds = 45,
            onPlayPauseClick = {},
            onSeek = {},
            onExit = {},
            onToggleControls = {},
            content = {
                Box(Modifier.fillMaxSize().background(Color.DarkGray))
            }
        )
    }
}

@Preview
@Composable
private fun VideoScreenWithAnimationPreview() {
    val screenSize = IntSize(1920, 1080)
    val frames = remember {
        // Generate 60 frames of animation (20 seconds at 3fps)
        (0 until 60).map { getMockSegmentFrame() }
    }

    var animation by remember { mutableStateOf<VideoDefinition?>(null) }

    LaunchedEffect(Unit) {
        animation = buildAnimation(frames, screenSize, fps = 3)
    }

    AppTheme {
        animation?.let { video ->
            val controller = rememberVideoController(video.duration, fps = 60)
            VideoScreen(
                animation = video,
                videoResolution = screenSize,
                videoController = controller,
                onExit = {}
            )
        } ?: Box(
            Modifier.fillMaxSize().background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", color = Color.White)
        }
    }
}

@Preview
@Composable
private fun SegmentFrameCanvasTestPreview() {
    // Direct test of SegmentFrameCanvas - no video player involved
    val screenSize = IntSize(1920, 1080)
    Box(
        Modifier.fillMaxSize().background(Color.White)
    ) {
        com.olcayaras.figures.SegmentFrameCanvas(
            modifier = Modifier.fillMaxSize(),
            frame = getMockSegmentFrame(),
            viewportSize = screenSize
        )
    }
}
