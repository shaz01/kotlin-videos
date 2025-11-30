package com.olcayaras.lib.videobuilders

import kotlin.time.Duration

class JsMediaResourceScope : MediaResourceScope {
    override suspend fun video(path: String, start: Duration, end: MediaEnd): MediaResource.Video {
        TODO("JS video implementation not yet available - requires WebCodecs API")
    }
}

actual fun createMediaResourceScope(fps: Int): MediaResourceScope {
    return JsMediaResourceScope()
}