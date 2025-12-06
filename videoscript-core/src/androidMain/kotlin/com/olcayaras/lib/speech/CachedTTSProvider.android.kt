package com.olcayaras.lib.speech

actual class CachedTTSProvider actual constructor(
    delegate: TTSProvider,
    cacheDirectory: String
) : TTSProvider {
    actual override suspend operator fun invoke(text: String): SpeechWithTimestamps {
        TODO("Not yet implemented")
    }
}