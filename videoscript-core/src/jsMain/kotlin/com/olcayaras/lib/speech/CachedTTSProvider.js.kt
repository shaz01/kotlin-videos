package com.olcayaras.lib.speech

import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256

actual class CachedTTSProvider actual constructor(
    private val delegate: TTSProvider,
    private val cacheDirectory: String
) : TTSProvider {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun textToFileName(text: String): String {
        val hash = SHA256().digest(text.encodeToByteArray()).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
        return "tts_cache_$hash"
    }

    private fun getFromCache(text: String): SpeechWithTimestamps? {
        val cacheKey = textToFileName(text)
        return try {
            val cachedJson = js("localStorage.getItem(cacheKey)") as? String
            if (cachedJson != null) {
                json.decodeFromString<SpeechWithTimestamps>(cachedJson)
            } else null
        } catch (e: Exception) {
            console.log("Error reading from cache: ${e.message}")
            null
        }
    }

    private fun saveToCache(text: String, speech: SpeechWithTimestamps) {
        val cacheKey = textToFileName(text)
        try {
            val jsonContent = json.encodeToString(speech)
            js("localStorage.setItem(cacheKey, jsonContent)")
        } catch (e: Exception) {
            console.log("Error saving to cache: ${e.message}")
        }
    }

    actual override suspend fun invoke(text: String): SpeechWithTimestamps {
        if (text.isEmpty()) return SpeechWithTimestamps("", emptyList(), emptyList(), emptyList())

        val cached = getFromCache(text)
        if (cached != null) return cached

        val generated = delegate(text)
        saveToCache(text, generated)
        return generated
    }
}