package com.director.elevenlabs

import com.olcayaras.elevenlabs.ElevenLabsClient
import com.olcayaras.elevenlabs.models.VoiceSettings
import com.olcayaras.lib.speech.SpeechWithTimestamps
import com.olcayaras.lib.speech.TTSProvider

class ElevenLabsTTSProvider(
    apiKey: String,
    val voiceId: String,
    val modelId: String,
    val languageCode: String? = null,
    val voiceSettings: VoiceSettings? = null,
) : TTSProvider {
    val client = ElevenLabsClient(apiKey)
    override suspend fun invoke(text: String): SpeechWithTimestamps {
        val speechData = client
            .convertTextToSpeechWithTimestamps(
                voiceId = voiceId,
                text = text,
                modelId = modelId,
                languageCode = languageCode,
                voiceSettings = voiceSettings
            )
            .getOrThrow()

        val result = SpeechWithTimestamps(
            audio = speechData.audioBase64,
            chars = speechData.normalizedAlignment.characters,
            startSeconds = speechData.normalizedAlignment.characterStartTimesSeconds,
            endSeconds = speechData.normalizedAlignment.characterEndTimesSeconds
        )
        return result
    }
}