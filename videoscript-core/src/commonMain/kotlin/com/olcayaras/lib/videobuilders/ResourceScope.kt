package com.olcayaras.lib.videobuilders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.olcayaras.lib.LocalIsRendering
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


sealed interface MediaResource {
    interface Video : MediaResource {
        @Composable
        fun currentVideoFrame(): ImageBitmap

        fun getLength(): Duration
    }
}

sealed interface MediaEnd {
    object UntilEnd : MediaEnd
    data class FixedDuration(val duration: Duration) : MediaEnd
    data class FixedPoint(val end: Duration) : MediaEnd

    fun getAbsolute(start: Duration, media: MediaResource.Video): Duration {
        return when (this) {
            is FixedDuration -> start + duration
            is FixedPoint -> end
            UntilEnd -> media.getLength()
        }
    }
}

interface MediaResourceScope {
    suspend fun video(path: String, start: Duration = 0.seconds, end: MediaEnd): MediaResource.Video
}

object NoOpMediaResourceScope : MediaResourceScope {
    override suspend fun video(
        path: String,
        start: Duration,
        end: MediaEnd
    ): MediaResource.Video {
        throw NotImplementedError("Video resources are not supported in NoOpMediaResourceScope (stub implementation)")
    }
}

expect fun createMediaResourceScope(fps: Int): MediaResourceScope


@Composable
fun EmbedVideo(modifier: Modifier = Modifier, video: MediaResource.Video) {
//    val isRendering = LocalIsRendering.current
    val image = video.currentVideoFrame()
    Image(
        modifier = modifier,
        bitmap = image,
        contentDescription = null
    )
}
