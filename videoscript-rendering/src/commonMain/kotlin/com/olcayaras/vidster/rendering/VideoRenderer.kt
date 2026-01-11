package com.olcayaras.vidster.rendering

import com.olcayaras.lib.definitions.AudioDefinition
import java.nio.file.Path
import kotlin.time.Duration

interface VideoRenderer {
    fun exportVideo(
        outputPath: Path,
        totalDuration: Duration,
        fps: Int = 30,
        withAlpha: Boolean = false,
        audio: List<AudioDefinition>,
        onFrameRendered: (frameIndex: Int, totalFrames: Int) -> Unit = { _, _ -> }
    )
}

expect fun createVideoRenderer(frameRenderer: FrameRenderer): VideoRenderer