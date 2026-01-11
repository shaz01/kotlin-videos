package com.olcayaras.vidster.rendering

import com.olcayaras.lib.definitions.AudioDefinition
import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Duration

interface VideoRenderer {
    fun exportVideo(
        outputPath: PlatformFile,
        totalDuration: Duration,
        fps: Int = 30,
        withAlpha: Boolean = false,
        audio: List<AudioDefinition>,
        onFrameRendered: (frameIndex: Int, totalFrames: Int) -> Unit = { _, _ -> }
    )
}

expect fun createVideoRenderer(frameRenderer: FrameRenderer): VideoRenderer