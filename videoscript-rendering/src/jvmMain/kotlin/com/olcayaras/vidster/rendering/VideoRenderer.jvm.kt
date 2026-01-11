package com.olcayaras.vidster.rendering

actual fun createVideoRenderer(frameRenderer: FrameRenderer): VideoRenderer {
    return FFmpegProcessVideoRenderer(frameRenderer)
}