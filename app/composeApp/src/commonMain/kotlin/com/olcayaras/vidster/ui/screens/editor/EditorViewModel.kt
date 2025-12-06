package com.olcayaras.vidster.ui.screens.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntSize
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.olcayaras.vidster.ViewModel
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.getMockSegmentFrame

sealed interface EditorEvent {
    data class SelectFrame(val frame: SegmentFrame) : EditorEvent
    data object AddFrame : EditorEvent
    data class RemoveFrame(val frame: SegmentFrame) : EditorEvent
}

data class EditorState(
    val frames: List<SegmentFrame>,
    val selectedFrame: SegmentFrame?,
    val screenSize: IntSize = IntSize(1920, 1080)
)

class EditorViewModel(c: ComponentContext) : ViewModel<EditorEvent, EditorState>(c) {
    private val _frames = MutableStateFlow<List<SegmentFrame>>(emptyList())
    private val _selectedFrame = MutableStateFlow<SegmentFrame?>(null)

    init {
        // Initialize with mock frames
//        val mockFrames = List(10) { getMockSegmentFrame() }
//        _frames.value = mockFrames
//        _selectedFrame.value = mockFrames.firstOrNull()
    }

    private fun selectFrame(frame: SegmentFrame) {
        _selectedFrame.value = frame
    }

    private fun addFrame() {
        val newFrame = getMockSegmentFrame()
        _frames.value += newFrame
    }

    private fun removeFrame(frame: SegmentFrame) {
        _frames.value -= frame
        if (_selectedFrame.value == frame) {
            _selectedFrame.value = _frames.value.firstOrNull()
        }
    }

    @Composable
    override fun models(events: Flow<EditorEvent>): EditorState {
        val frames by _frames.collectAsState()
        val selectedFrame by _selectedFrame.collectAsState()

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is EditorEvent.SelectFrame -> selectFrame(event.frame)
                    is EditorEvent.AddFrame -> addFrame()
                    is EditorEvent.RemoveFrame -> removeFrame(event.frame)
                }
            }
        }

        return EditorState(frames, selectedFrame)
    }
}