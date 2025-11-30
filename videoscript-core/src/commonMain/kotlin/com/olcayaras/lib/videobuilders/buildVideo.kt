package com.olcayaras.lib.videobuilders

import com.olcayaras.lib.definitions.AudioDefinition
import com.olcayaras.lib.definitions.SequenceDefinition
import com.olcayaras.lib.definitions.VideoDefinition
import com.olcayaras.lib.speech.TTSProvider

suspend fun buildVideo(ttsProvider: TTSProvider, fps: Int = 60, definition: suspend SequenceScope.() -> Unit): VideoDefinition {
    val scope = SequenceScopeNewImpl(fps, ttsProvider)
    scope.definition()
    val compsWithTimings = scope.build()
    val realComponents = compsWithTimings.map {
        val (timing, def) = it
        val from = timing.from
        val end = timing.absoluteEnd()!!

        when(def) {
            is AudioDef.TTS -> AudioDefinition.TTS(from, end, def.audioBase64, def.text, def.speechWithTimestamps)
            is AudioDef.Resource -> AudioDefinition.Resource(from, end, def.file, def.mediaFrom, def.mediaTo)
            is SequenceDef -> SequenceDefinition(from, end, def.enter, def.exit, def.content, def.tag)
        }
    }

    return VideoDefinition(realComponents)
}

fun defineVideo(builder: suspend SequenceScope.() -> Unit): suspend SequenceScope.() -> Unit = builder
