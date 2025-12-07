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
import com.olcayaras.figures.CanvasState
import com.olcayaras.figures.Figure
import com.olcayaras.figures.FigureFrame
import com.olcayaras.figures.Joint
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.Viewport
import com.olcayaras.figures.deepCopy
import com.olcayaras.figures.getMockFigure
import io.github.aakira.napier.Napier

sealed interface EditorEvent {
    // Frame management
    data class SelectFrame(val index: Int) : EditorEvent
    data object AddFrame : EditorEvent
    data class RemoveFrame(val index: Int) : EditorEvent

    // Canvas operations
    data class UpdateCanvasState(val canvasState: CanvasState) : EditorEvent

    // Figure editing
    data class UpdateJointAngle(val figure: Figure, val joint: Joint, val newAngle: Float) : EditorEvent
    data class MoveFigure(val figure: Figure, val newX: Float, val newY: Float) : EditorEvent

    // Playback
    data object PlayAnimation : EditorEvent
}

data class EditorState(
    val frames: List<FigureFrame>,
    val selectedFrameIndex: Int,
    val canvasState: CanvasState,
    val figureModificationCount: Long = 0L,
    val screenSize: IntSize = IntSize(1920, 1080)
) {
    val selectedFrame: FigureFrame? get() = frames.getOrNull(selectedFrameIndex)

    val selectedFigures: List<Figure> get() = selectedFrame?.figures ?: emptyList()

    // Compile selected frame for timeline preview
    val selectedSegmentFrame: SegmentFrame? = selectedFrame?.compile()

    // Compile all frames for timeline thumbnails
    val segmentFrames: List<SegmentFrame> = frames.map { it.compile() }
}

class EditorViewModel(
    c: ComponentContext,
    private val onPlayAnimation: (frames: List<SegmentFrame>, screenSize: IntSize) -> Unit = { _, _ -> }
) : ViewModel<EditorEvent, EditorState>(c) {
    private val _frames = MutableStateFlow<List<FigureFrame>>(emptyList())
    private val _selectedFrameIndex = MutableStateFlow(0)
    private val _canvasState = MutableStateFlow(CanvasState())
    private val _figureModificationCount = MutableStateFlow(0L)

    init {
        // Initialize with a single frame containing a mock figure
        val initialFrame = FigureFrame(
            figures = listOf(getMockFigure(x = 400f, y = 300f)),
            viewport = Viewport()
        )
        _frames.value = listOf(initialFrame)
    }

    private fun selectFrame(index: Int) {
        if (index in _frames.value.indices) {
            _selectedFrameIndex.value = index
        } else {
            Napier.e { "Invalid frame index: $index" }
        }
    }

    private fun addFrame() {
        // Clone the current frame or create a new one
        val currentFrame = _frames.value.getOrNull(_selectedFrameIndex.value)
        val newFrame = currentFrame?.deepCopy()
            ?: _frames.value.lastOrNull()?.deepCopy()
            ?: FigureFrame(
                figures = listOf(getMockFigure(x = 400f, y = 300f)),
                viewport = Viewport()
            )

        _frames.value += newFrame
        _selectedFrameIndex.value = _frames.value.lastIndex
    }

    private fun removeFrame(index: Int) {
        if (_frames.value.size <= 1) return // Keep at least one frame

        val newFrames = _frames.value.toMutableList()
        newFrames.removeAt(index)
        _frames.value = newFrames

        // Adjust selection
        if (_selectedFrameIndex.value >= newFrames.size) {
            _selectedFrameIndex.value = newFrames.lastIndex
        }
    }

    private fun updateCanvasState(state: CanvasState) {
        _canvasState.value = state
    }

    private fun updateJointAngle(joint: Joint, newAngle: Float) {
        // Update the joint's angle directly (Joint has mutable angle)
        joint.angle = newAngle

        // Increment modification count to trigger recomposition
        _figureModificationCount.value++
    }

    private fun moveFigure(figure: Figure, newX: Float, newY: Float) {
        // Update figure position directly (Figure has mutable x/y)
        figure.x = newX
        figure.y = newY

        // Increment modification count to trigger recomposition
        _figureModificationCount.value++
    }

    private fun playAnimation(screenSize: IntSize) {
        val segmentFrames = _frames.value.map { it.compile() }
        onPlayAnimation(segmentFrames, screenSize)
    }

    @Composable
    override fun models(events: Flow<EditorEvent>): EditorState {
        val frames by _frames.collectAsState()
        val selectedFrameIndex by _selectedFrameIndex.collectAsState()
        val canvasState by _canvasState.collectAsState()
        val figureModificationCount by _figureModificationCount.collectAsState()
        val screenSize = IntSize(1920, 1080)

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is EditorEvent.SelectFrame -> selectFrame(event.index)
                    is EditorEvent.AddFrame -> addFrame()
                    is EditorEvent.RemoveFrame -> removeFrame(event.index)
                    is EditorEvent.UpdateCanvasState -> updateCanvasState(event.canvasState)
                    is EditorEvent.UpdateJointAngle -> updateJointAngle(event.joint, event.newAngle)
                    is EditorEvent.MoveFigure -> moveFigure(event.figure, event.newX, event.newY)
                    is EditorEvent.PlayAnimation -> playAnimation(screenSize)
                }
            }
        }

        return EditorState(frames, selectedFrameIndex, canvasState, figureModificationCount, screenSize)
    }
}
