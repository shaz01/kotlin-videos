package com.olcayaras.lib.subtitles

import androidx.compose.runtime.Immutable
import com.olcayaras.lib.speech.SpeechWithTimestamps
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Immutable
@Serializable
data class Subtitle(
    val text: String,
    val startTime: Duration,
    val endTime: Duration,
    val wordTimings: List<WordTiming>,
    val chars: List<String>,
    val charStarts: List<Double>,
    val charEnds: List<Double>,
)

@Immutable
@Serializable
data class WordTiming(
    val text: String,
    val startIndex: Int,
    val endIndex: Int, // Exclusive
    val startTime: Duration,
    val endTime: Duration
)

fun SpeechWithTimestamps.generateSubtitles(
    maxDuration: Duration = 4.seconds,
    pauseThreshold: Duration = 500.milliseconds,
    maxCharsPerLine: Int = 50
): List<Subtitle> {
    val words = extractWords()
    if (words.isEmpty()) return emptyList()

    return createSubtitleChunks(this, words, maxDuration, pauseThreshold, maxCharsPerLine)
}

private fun SpeechWithTimestamps.extractWords(): List<WordTiming> {
    val wordTimings = mutableListOf<WordTiming>()
    val currentWord = StringBuilder()
    var wordStartIndex = -1

    for (i in chars.indices) {
        val char = chars[i]

        when {
            char.isBlank() || char in ".,!?;:" -> {
                // End of word
                if (currentWord.isNotEmpty() && wordStartIndex >= 0) {
                    val wordEndIndex = if (char.isBlank()) i - 1 else i
                    val wordText = currentWord.toString().trim()
                    if (wordText.isNotEmpty()) {
                        wordTimings.add(
                            WordTiming(
                                text = wordText,
                                startTime = startSeconds[wordStartIndex].seconds,
                                endTime = endSeconds[wordEndIndex].seconds,
                                startIndex = wordStartIndex,
                                endIndex = wordEndIndex
                            )
                        )
                    }
                    currentWord.clear()
                    wordStartIndex = -1
                }
            }

            else -> {
                // Part of a word
                if (wordStartIndex == -1) {
                    wordStartIndex = i
                }
                currentWord.append(char)
            }
        }
    }

    // Handle last word if it doesn't end with punctuation/whitespace
    if (currentWord.isNotEmpty() && wordStartIndex >= 0) {
        wordTimings.add(
            WordTiming(
                text = currentWord.toString().trim(),
                startTime = startSeconds[wordStartIndex].seconds,
                endTime = endSeconds.last().seconds,
                startIndex = wordStartIndex,
                endIndex = chars.lastIndex
            )
        )
    }

    return wordTimings
}


private fun createSubtitleChunks(
    speechWithTimestamps: SpeechWithTimestamps,
    wordTimings: List<WordTiming>,
    maxDuration: Duration,
    pauseThreshold: Duration,
    maxCharsPerLine: Int
): List<Subtitle> {
    val subtitles = mutableListOf<Subtitle>()
    val currentChunkWordTimings = mutableListOf<WordTiming>()

    for (i in wordTimings.indices) {
        val word = wordTimings[i]
        currentChunkWordTimings.add(word)

        val chunkLength = currentChunkWordTimings.last().endIndex - currentChunkWordTimings.first().startIndex + 1
        val chunkDuration = word.endTime - currentChunkWordTimings.first().startTime

        val shouldBreak = when {
            // Text too long
            chunkLength > maxCharsPerLine -> true
            // Duration too long
            chunkDuration > maxDuration -> true
            // Natural pause detected
            i < wordTimings.size - 1 && wordTimings[i + 1].startTime - word.endTime > pauseThreshold -> true
            // Last word
            i == wordTimings.size - 1 -> true
            else -> false
        }

        if (shouldBreak) {
            val subtitle = createSubtitle(speechWithTimestamps, currentChunkWordTimings)
            subtitles.add(subtitle)
            currentChunkWordTimings.clear()
        }
    }

    return subtitles
}

private fun createSubtitle(speech: SpeechWithTimestamps, wordTimings: List<WordTiming>): Subtitle {
    val startTime = wordTimings.first().startTime
    val endTime = wordTimings.last().endTime

    val textStart = wordTimings.first().startIndex
    val textEnd = wordTimings.last().endIndex + 1 // add one because sublist is exclusive to end

    val chars = speech.chars.subList(textStart, textEnd)
    val charStarts = speech.startSeconds.subList(textStart, textEnd)
    val charEnds = speech.endSeconds.subList(textStart, textEnd)

    val text = chars.joinToString(separator = "")
    return Subtitle(
        text = text,
        startTime = startTime,
        endTime = endTime,
        wordTimings = wordTimings.map { it.copy(startIndex = it.startIndex - textStart, endIndex = it.endIndex - textStart) },
        chars = chars,
        charStarts = charStarts,
        charEnds = charEnds
    )
}

