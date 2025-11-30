package com.olcayaras.lib.speech

expect class CachedTTSProvider(
    delegate: TTSProvider,
    cacheDirectory: String = "tts-cache"
) : TTSProvider {
    override suspend fun invoke(text: String): SpeechWithTimestamps
}