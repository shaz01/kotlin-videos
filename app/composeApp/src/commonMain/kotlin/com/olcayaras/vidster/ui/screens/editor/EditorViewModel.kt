package com.olcayaras.vidster.ui.screens.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntSize
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.olcayaras.vidster.ViewModel
import com.olcayaras.figures.CanvasState
import com.olcayaras.figures.CompiledJoint
import com.olcayaras.figures.Figure
import com.olcayaras.figures.FigureFrame
import com.olcayaras.figures.Joint
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.Viewport
import com.olcayaras.figures.compileForEditing
import com.olcayaras.figures.deepCopy
import com.olcayaras.figures.getMockFigure
import com.olcayaras.figures.getMockShapesDemo
import com.olcayaras.vidster.ui.Route
import io.github.aakira.napier.Napier

enum class OnionSkinMode {
    Disabled,
    Previous,
    Future,
    Both
}

sealed interface EditorEvent {
    // Frame management
    data class SelectFrame(val index: Int) : EditorEvent
    data object AddFrame : EditorEvent
    data class RemoveFrame(val index: Int) : EditorEvent
    data class ReorderFrames(val fromIndex: Int, val toIndex: Int) : EditorEvent
    data class InsertFrameAt(val index: Int) : EditorEvent
    data class DuplicateFrame(val index: Int) : EditorEvent

    // Selection mode
    data object EnterSelectionMode : EditorEvent
    data object ExitSelectionMode : EditorEvent
    data class ToggleFrameSelection(val index: Int) : EditorEvent
    data object DeleteSelectedFrames : EditorEvent

    // Canvas operations
    data class UpdateCanvasState(val canvasState: CanvasState) : EditorEvent

    // Viewport operations
    data class UpdateViewport(val viewport: Viewport) : EditorEvent
    data object BeginViewportScaleChange : EditorEvent
    data class UpdateViewportScale(val scale: Float) : EditorEvent

    // Figure editing - Begin events push undo snapshot, Update events don't
    data class BeginJointDrag(val figure: Figure, val joint: Joint) : EditorEvent
    data class UpdateJointAngle(val figure: Figure, val joint: Joint, val newAngle: Float) : EditorEvent
    data class BeginFigureMove(val figure: Figure) : EditorEvent
    data class MoveFigure(val figure: Figure, val newX: Float, val newY: Float) : EditorEvent
    data object BeginViewportDrag : EditorEvent

    // Figure management
    data class EditFigure(val figureIndex: Int) : EditorEvent
    data object AddNewFigure : EditorEvent

    // Playback
    data object PlayAnimation : EditorEvent

    // Onion skinning
    data class SetOnionSkinMode(val mode: OnionSkinMode) : EditorEvent

    // Undo/Redo
    data object Undo : EditorEvent
    data object Redo : EditorEvent
}

/**
 * Represents a frame to be rendered as onion skin with a specific opacity.
 * @param isPrevious true for previous frames (shown in red), false for next frames (shown in blue)
 */
data class OnionSkinFrame(
    val compiledJoints: List<CompiledJoint>,
    val alpha: Float,
    val isPrevious: Boolean
)

data class EditorState(
    val frames: List<FigureFrame>,
    val selectedFrameIndex: Int,
    val canvasState: CanvasState,
    val figureModificationCount: Long = 0L,
    val viewportSize: IntSize,
    val onionSkinMode: OnionSkinMode = OnionSkinMode.Disabled,
    val onionSkinPreviousCount: Int = 2,
    val onionSkinNextCount: Int = 1,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedFrameIndices: Set<Int> = emptySet()
) {
    val selectedFrame: FigureFrame get() {
        val frame = frames.getOrNull(selectedFrameIndex) ?: frames.lastOrNull() ?: throw IllegalStateException("No frames: ${frames.size}, selected index: $selectedFrameIndex, $frames")
        return frame
    }

    val selectedFigures: List<Figure> get() = selectedFrame.figures

    // Compile all frames for timeline thumbnails
    val segmentFrames: List<SegmentFrame> = frames.map { it.compile() }

    /**
     * Computes onion skin frames with decreasing opacity.
     * Previous frames are shown in red tint, next frames in blue tint.
     */
    val onionSkinFrames: List<OnionSkinFrame> get() {
        if (onionSkinMode == OnionSkinMode.Disabled) return emptyList()

        val result = mutableListOf<OnionSkinFrame>()
        val showPrevious = onionSkinMode == OnionSkinMode.Previous || onionSkinMode == OnionSkinMode.Both
        val showFuture = onionSkinMode == OnionSkinMode.Future || onionSkinMode == OnionSkinMode.Both

        // Previous frames (closer frames have higher opacity)
        if (showPrevious) {
            for (i in 1..onionSkinPreviousCount) {
                val frameIndex = selectedFrameIndex - i
                if (frameIndex >= 0) {
                    val frame = frames[frameIndex]
                    val alpha = 0.4f * (1f - (i - 1).toFloat() / onionSkinPreviousCount)
                    val compiledJoints = frame.figures.flatMap { it.compileForEditing() }
                    result.add(OnionSkinFrame(compiledJoints, alpha, isPrevious = true))
                }
            }
        }

        // Next frames (closer frames have higher opacity)
        if (showFuture) {
            for (i in 1..onionSkinNextCount) {
                val frameIndex = selectedFrameIndex + i
                if (frameIndex < frames.size) {
                    val frame = frames[frameIndex]
                    val alpha = 0.4f * (1f - (i - 1).toFloat() / onionSkinNextCount)
                    val compiledJoints = frame.figures.flatMap { it.compileForEditing() }
                    result.add(OnionSkinFrame(compiledJoints, alpha, isPrevious = false))
                }
            }
        }

        return result
    }
}

class EditorViewModel(
    c: ComponentContext,
    private val navigation: StackNavigation<Route>,
) : ViewModel<EditorEvent, EditorState>(c) {
    private val _frames = MutableStateFlow<List<FigureFrame>>(emptyList())
    private val _selectedFrameIndex = MutableStateFlow(0)
    private val _canvasState = MutableStateFlow(CanvasState())
    private val _figureModificationCount = MutableStateFlow(0L)
    private val _onionSkinMode = MutableStateFlow(OnionSkinMode.Disabled)

    // Selection mode
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedFrameIndices = MutableStateFlow<Set<Int>>(emptySet())

    // Undo/Redo history stacks
    private val _undoStack = MutableStateFlow<List<List<FigureFrame>>>(emptyList())
    private val _redoStack = MutableStateFlow<List<List<FigureFrame>>>(emptyList())
    private val maxHistorySize = 50

    init {
        // Initialize with a single frame containing a mock figure
        val initialFrame = FigureFrame(
            figures = listOf(
                getMockFigure(x = 400f, y = 300f),
                getMockShapesDemo(x = 700f, y = 300f)
            ),
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
        pushUndoSnapshot()
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

        pushUndoSnapshot()
        val newFrames = _frames.value.toMutableList()
        newFrames.removeAt(index)
        _frames.value = newFrames

        // Adjust selection
        if (_selectedFrameIndex.value >= newFrames.size) {
            _selectedFrameIndex.value = newFrames.lastIndex
        }
    }

    private fun reorderFrames(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _frames.value.indices || toIndex !in _frames.value.indices) return

        pushUndoSnapshot()
        val originalSelectedIndex = _selectedFrameIndex.value
        val newFrames = _frames.value.toMutableList()
        val movedFrame = newFrames.removeAt(fromIndex)
        newFrames.add(toIndex, movedFrame)
        _frames.value = newFrames

        // Update selection based on how the reorder affects the originally selected frame
        val newSelectedIndex = when {
            // If the selected frame is the one being moved, selection follows it
            originalSelectedIndex == fromIndex -> toIndex
            // Moving forward: frames between fromIndex and toIndex shift left by 1
            fromIndex < toIndex && originalSelectedIndex in (fromIndex + 1)..toIndex -> originalSelectedIndex - 1
            // Moving backward: frames between toIndex (inclusive) and fromIndex (exclusive) shift right by 1
            toIndex < fromIndex && originalSelectedIndex in toIndex until fromIndex -> originalSelectedIndex + 1
            // Otherwise, selection is unaffected
            else -> originalSelectedIndex
        }
        _selectedFrameIndex.value = newSelectedIndex
    }

    private fun insertFrameAt(index: Int) {
        pushUndoSnapshot()
        val insertIndex = index.coerceIn(0, _frames.value.size)

        // Clone the frame at the insertion point (or previous frame)
        val templateFrame = _frames.value.getOrNull(insertIndex)
            ?: _frames.value.getOrNull(insertIndex - 1)
            ?: FigureFrame(
                figures = listOf(getMockFigure(x = 400f, y = 300f)),
                viewport = Viewport()
            )

        val newFrame = templateFrame.deepCopy()
        val newFrames = _frames.value.toMutableList()
        newFrames.add(insertIndex, newFrame)
        _frames.value = newFrames
        _selectedFrameIndex.value = insertIndex
    }

    private fun duplicateFrame(index: Int) {
        if (index !in _frames.value.indices) return

        pushUndoSnapshot()
        val sourceFrame = _frames.value[index]
        val duplicatedFrame = sourceFrame.deepCopy()
        val newFrames = _frames.value.toMutableList()
        newFrames.add(index + 1, duplicatedFrame)
        _frames.value = newFrames
        _selectedFrameIndex.value = index + 1
    }

    // Selection mode handlers
    private fun enterSelectionMode() {
        _selectionMode.value = true
        _selectedFrameIndices.value = emptySet()
    }

    private fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedFrameIndices.value = emptySet()
    }

    private fun toggleFrameSelection(index: Int) {
        if (index !in _frames.value.indices) return

        val currentSelection = _selectedFrameIndices.value
        _selectedFrameIndices.value = if (index in currentSelection) {
            currentSelection - index
        } else {
            currentSelection + index
        }
    }

    private fun deleteSelectedFrames() {
        val selectedIndices = _selectedFrameIndices.value
        if (selectedIndices.isEmpty()) return

        // Ensure at least one frame remains
        val framesToKeep = _frames.value.size - selectedIndices.size
        if (framesToKeep < 1) return

        pushUndoSnapshot()
        val newFrames = _frames.value.filterIndexed { index, _ -> index !in selectedIndices }
        _frames.value = newFrames

        // Adjust current selection
        if (_selectedFrameIndex.value >= newFrames.size) {
            _selectedFrameIndex.value = newFrames.lastIndex.coerceAtLeast(0)
        }

        // Exit selection mode
        exitSelectionMode()
    }

    private fun updateCanvasState(state: CanvasState) {
        _canvasState.value = state
    }

    private fun updateJointAngle(joint: Joint, newAngle: Float) {
        // Update the joint's angle directly (Joint has mutable angle)
        // Note: Snapshot is pushed in beginJointDrag(), not here
        joint.angle = newAngle

        // Increment modification count to trigger recomposition
        _figureModificationCount.value++
    }

    private fun moveFigure(figure: Figure, newX: Float, newY: Float) {
        // Update figure position directly (Figure has mutable x/y)
        // Note: Snapshot is pushed in beginFigureMove(), not here
        figure.x = newX
        figure.y = newY

        // Increment modification count to trigger recomposition
        _figureModificationCount.value++
    }

    private fun updateViewport(viewport: Viewport) {
        // Update the selected frame's viewport
        // Note: Snapshot is pushed in beginViewportDrag(), not here
        val frameIndex = _selectedFrameIndex.value
        val currentFrames = _frames.value.toMutableList()
        if (frameIndex in currentFrames.indices) {
            val frame = currentFrames[frameIndex]
            currentFrames[frameIndex] = frame.copy(viewport = viewport)
            _frames.value = currentFrames
        }
    }

    private fun beginViewportScaleChange() {
        pushUndoSnapshot()
    }

    private fun updateViewportScale(scale: Float) {
        // Update the selected frame's viewport scale (zoom)
        // Note: Snapshot is pushed in beginViewportScaleChange(), not here
        val frameIndex = _selectedFrameIndex.value
        val currentFrames = _frames.value.toMutableList()
        if (frameIndex in currentFrames.indices) {
            val frame = currentFrames[frameIndex]
            val newViewport = frame.viewport.copy(scale = scale.coerceIn(0.5f, 2.0f))
            currentFrames[frameIndex] = frame.copy(viewport = newViewport)
            _frames.value = currentFrames
        }
    }

    private fun playAnimation(screenSize: IntSize) {
        val segmentFrames = _frames.value.map { it.compile() }
        navigation.bringToFront(
            Route.Video(
                segmentFrames,
                videoScreenWidth = screenSize.width,
                videoScreenHeight = screenSize.height
            )
        )
    }

    private fun editFigure(figureIndex: Int) {
        val frame = _frames.value.getOrNull(_selectedFrameIndex.value) ?: return
        val figure = frame.figures.getOrNull(figureIndex) ?: return

        navigation.bringToFront(
            Route.EditFigure(
                figure = figure.deepCopy(),
                figureIndex = figureIndex,
                onFinish = { resultFigure ->
                    resultFigure?.let { updateFigure(it, figureIndex) }
                }
            )
        )
    }

    private fun addNewFigure() {
        navigation.bringToFront(
            Route.EditFigure(
                figure = null,
                figureIndex = null,
                onFinish = { resultFigure ->
                    resultFigure?.let { addFigure(it) }
                }
            )
        )
    }

    private fun updateFigure(figure: Figure, figureIndex: Int) {
        val frameIndex = _selectedFrameIndex.value
        val currentFrames = _frames.value.toMutableList()
        if (frameIndex !in currentFrames.indices) return

        pushUndoSnapshot()
        val frame = currentFrames[frameIndex]
        val newFigures = frame.figures.toMutableList()
        if (figureIndex in newFigures.indices) {
            newFigures[figureIndex] = figure
            currentFrames[frameIndex] = frame.copy(figures = newFigures)
            _frames.value = currentFrames
            _figureModificationCount.value++
        }
    }

    private fun addFigure(figure: Figure) {
        val frameIndex = _selectedFrameIndex.value
        val currentFrames = _frames.value.toMutableList()
        if (frameIndex !in currentFrames.indices) return

        pushUndoSnapshot()
        val frame = currentFrames[frameIndex]
        val newFigures = frame.figures.toMutableList()
        newFigures.add(figure)
        currentFrames[frameIndex] = frame.copy(figures = newFigures)
        _frames.value = currentFrames
        _figureModificationCount.value++
    }

    private fun setOnionSkinMode(mode: OnionSkinMode) {
        _onionSkinMode.value = mode
    }

    // Drag start handlers - push snapshot once at the start of a drag operation
    private fun beginJointDrag() {
        pushUndoSnapshot()
    }

    private fun beginFigureMove() {
        pushUndoSnapshot()
    }

    private fun beginViewportDrag() {
        pushUndoSnapshot()
    }

    // Undo/Redo helper functions

    private fun pushUndoSnapshot() {
        val snapshot = _frames.value.map { it.deepCopy() }
        _undoStack.value = (_undoStack.value + listOf(snapshot)).takeLast(maxHistorySize)
        _redoStack.value = emptyList() // Clear redo on new action
    }

    private fun undo() {
        val undoStack = _undoStack.value
        if (undoStack.isEmpty()) return

        // Push current state to redo
        val currentSnapshot = _frames.value.map { it.deepCopy() }
        _redoStack.value += listOf(currentSnapshot)

        // Restore previous state
        val previousState = undoStack.last()
        _undoStack.value = undoStack.dropLast(1)
        _frames.value = previousState

        // Adjust selection if needed
        if (_selectedFrameIndex.value >= _frames.value.size) {
            _selectedFrameIndex.value = _frames.value.lastIndex.coerceAtLeast(0)
        }

        // Increment modification count to trigger recomposition
        _figureModificationCount.value++
    }

    private fun redo() {
        val redoStack = _redoStack.value
        if (redoStack.isEmpty()) return

        // Push current state to undo
        val currentSnapshot = _frames.value.map { it.deepCopy() }
        _undoStack.value += listOf(currentSnapshot)

        // Restore next state
        val nextState = redoStack.last()
        _redoStack.value = redoStack.dropLast(1)
        _frames.value = nextState

        // Adjust selection if needed
        if (_selectedFrameIndex.value >= _frames.value.size) {
            _selectedFrameIndex.value = _frames.value.lastIndex.coerceAtLeast(0)
        }

        // Increment modification count to trigger recomposition
        _figureModificationCount.value++
    }

    @Composable
    override fun models(events: Flow<EditorEvent>): EditorState {
        val frames by _frames.collectAsState()
        val selectedFrameIndex by _selectedFrameIndex.collectAsState()
        val canvasState by _canvasState.collectAsState()
        val figureModificationCount by _figureModificationCount.collectAsState()
        val onionSkinMode by _onionSkinMode.collectAsState()
        val undoStack by _undoStack.collectAsState()
        val redoStack by _redoStack.collectAsState()
        val selectionMode by _selectionMode.collectAsState()
        val selectedFrameIndices by _selectedFrameIndices.collectAsState()
        val screenSize = IntSize(1920, 1080)

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is EditorEvent.SelectFrame -> if (!selectionMode) selectFrame(event.index)
                    is EditorEvent.AddFrame -> addFrame()
                    is EditorEvent.RemoveFrame -> removeFrame(event.index)
                    is EditorEvent.ReorderFrames -> reorderFrames(event.fromIndex, event.toIndex)
                    is EditorEvent.InsertFrameAt -> insertFrameAt(event.index)
                    is EditorEvent.DuplicateFrame -> duplicateFrame(event.index)
                    is EditorEvent.EnterSelectionMode -> enterSelectionMode()
                    is EditorEvent.ExitSelectionMode -> exitSelectionMode()
                    is EditorEvent.ToggleFrameSelection -> toggleFrameSelection(event.index)
                    is EditorEvent.DeleteSelectedFrames -> deleteSelectedFrames()
                    is EditorEvent.UpdateCanvasState -> updateCanvasState(event.canvasState)
                    is EditorEvent.BeginViewportDrag -> beginViewportDrag()
                    is EditorEvent.UpdateViewport -> updateViewport(event.viewport)
                    is EditorEvent.BeginViewportScaleChange -> beginViewportScaleChange()
                    is EditorEvent.UpdateViewportScale -> updateViewportScale(event.scale)
                    is EditorEvent.BeginJointDrag -> beginJointDrag()
                    is EditorEvent.UpdateJointAngle -> updateJointAngle(event.joint, event.newAngle)
                    is EditorEvent.BeginFigureMove -> beginFigureMove()
                    is EditorEvent.MoveFigure -> moveFigure(event.figure, event.newX, event.newY)
                    is EditorEvent.EditFigure -> editFigure(event.figureIndex)
                    is EditorEvent.AddNewFigure -> addNewFigure()
                    is EditorEvent.PlayAnimation -> playAnimation(screenSize)
                    is EditorEvent.SetOnionSkinMode -> setOnionSkinMode(event.mode)
                    is EditorEvent.Undo -> undo()
                    is EditorEvent.Redo -> redo()
                }
            }
        }

        return EditorState(
            frames = frames,
            selectedFrameIndex = selectedFrameIndex,
            canvasState = canvasState,
            figureModificationCount = figureModificationCount,
            viewportSize = screenSize,
            onionSkinMode = onionSkinMode,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            selectionMode = selectionMode,
            selectedFrameIndices = selectedFrameIndices
        )
    }
}
