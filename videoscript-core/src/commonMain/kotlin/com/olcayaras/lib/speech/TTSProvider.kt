package com.olcayaras.lib.speech

interface TTSProvider {
    suspend operator fun invoke(text: String): SpeechWithTimestamps
}

fun TTSProvider.asCachedTTSProvider() = this as? CachedTTSProvider ?: CachedTTSProvider(this)

class NoOpTTSProvider : TTSProvider {
    override suspend fun invoke(text: String): SpeechWithTimestamps {
        val chars = text.split("")
        return SpeechWithTimestamps(
            audio = "",
            chars = chars,
            startSeconds = chars.mapIndexed { index, _ -> index.toDouble() * 0.08 },
            endSeconds = chars.mapIndexed { index, _ -> index.toDouble() * 0.09 }
        )
    }
}