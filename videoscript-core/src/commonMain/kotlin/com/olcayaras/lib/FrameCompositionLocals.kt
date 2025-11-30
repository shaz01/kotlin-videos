package com.olcayaras.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.olcayaras.lib.videobuilders.SequenceScope
import kotlin.time.Duration

val LocalIsRendering = compositionLocalOf { false }
val LocalCurrentFrame = compositionLocalOf { 0 }
val LocalFPS = compositionLocalOf { -1 }

@Composable
fun currentFrame(): Int = LocalCurrentFrame.current

@Composable
fun SequenceScope.currentDuration(): Duration = currentAbsoluteDuration() - this.absoluteStart

@Composable
fun currentAbsoluteDuration(): Duration = currentFrame().frames.toDuration()

@Composable
fun firstFrame(): Int {
    val firstFrame = LocalCurrentFrame.current
    var cachedFrame by remember { mutableStateOf<Int?>(null) }

    if (cachedFrame == null) {
        cachedFrame = firstFrame
    }

    return cachedFrame!!
}

@Composable
fun firstDuration(): Duration = firstFrame().frames.toDuration()

@Composable
fun countFrames(): Int {
    val currentFrame = LocalCurrentFrame.current
    val firstFrame = firstFrame()

    return currentFrame - firstFrame
}

@Composable
fun progressBetween(start: Duration, end: Duration): Float {
    return (currentFrame() - start.ofFrames).toFloat() / end.ofFrames.toFloat()
}

@Composable
fun fps(): Int = LocalFPS.current
