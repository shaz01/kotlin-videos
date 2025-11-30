package com.olcayaras.lib.speech

import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import org.kotlincrypto.hash.sha2.SHA256

actual class CachedTTSProvider actual constructor(
    private val delegate: TTSProvider,
    private val cacheDirectory: String
) : TTSProvider {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val cacheDir = Path(cacheDirectory).apply {
        if (!SystemFileSystem.exists(this)) {
            SystemFileSystem.createDirectories(this)
        }
    }

    private fun textToFileName(text: String): String {
        val hash = SHA256().digest(text.encodeToByteArray()).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
        return "$hash.json"
    }

    private fun getFromCache(text: String): SpeechWithTimestamps? {
        val cacheFile = Path(cacheDir, textToFileName(text))
        if (!SystemFileSystem.exists(cacheFile)) return null

        return try {
            val jsonContent = SystemFileSystem.source(cacheFile).buffered().use { it.readString() }
            json.decodeFromString<SpeechWithTimestamps>(jsonContent)
        } catch (e: Exception) {
            println("Error reading from cache: ${e.message}. ${e.stackTraceToString()}")
            null
        }
    }

    private fun saveToCache(text: String, speech: SpeechWithTimestamps) {
        val cacheFile = Path(cacheDir, textToFileName(text))
        try {
            val jsonContent = json.encodeToString(speech)
            SystemFileSystem.sink(cacheFile).buffered().use { it.writeString(jsonContent) }
        } catch (e: Exception) {
            println("Error saving to cache: ${e.message}. ${e.stackTraceToString()}")
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