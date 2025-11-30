package com.olcayaras.lib.videobuilders

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class JvmMediaResourceScope(val fps: Int) : MediaResourceScope {
    
    private val loadedVideos = mutableListOf<FFmpegVideoResource>()
    
    override suspend fun video(
        path: String, 
        start: Duration, 
        end: MediaEnd
    ): MediaResource.Video {
        val videoResource = FFmpegVideoResource.create(path, fps, start, end)
        loadedVideos.add(videoResource)
        return videoResource
    }
    
    fun cleanup() {
        loadedVideos.forEach { it.cleanup() }
        loadedVideos.clear()
    }
}