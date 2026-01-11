package com.olcayaras.vidster.ui.screens.editfigure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.olcayaras.vidster.ViewModel
import com.olcayaras.figures.CanvasState
import com.olcayaras.figures.CompiledJoint
import com.olcayaras.figures.Figure
import com.olcayaras.figures.Joint
import com.olcayaras.figures.SegmentType
import com.olcayaras.figures.compileForEditing
import com.olcayaras.figures.deepCopy
import com.olcayaras.figures.findJointById
import com.olcayaras.figures.generateUniqueJointId
import com.olcayaras.figures.removeJoint

sealed interface EditFigureEvent {
    // Joint selection
    data class SelectJoint(val jointId: String?) : EditFigureEvent

    // Joint property editing (for selected joint)
    data class UpdateJointLength(val length: Float) : EditFigureEvent
    data class UpdateJointLengthForJoint(val jointId: String, val length: Float) : EditFigureEvent
    data class UpdateJointAngle(val angle: Float) : EditFigureEvent
    data class UpdateJointType(val type: SegmentType) : EditFigureEvent
    data class UpdateJointId(val newId: String) : EditFigureEvent

    // Hierarchy manipulation
    data class AddChildJoint(val parentId: String, val type: SegmentType) : EditFigureEvent
    data object DeleteSelectedJoint : EditFigureEvent

    // Canvas interaction
    data class UpdateCanvasState(val canvasState: CanvasState) : EditFigureEvent
    data class RotateJoint(val joint: Joint, val newAngle: Float) : EditFigureEvent
    data class MoveFigure(val newX: Float, val newY: Float) : EditFigureEvent

    // Figure name
    data class UpdateFigureName(val name: String) : EditFigureEvent

    // Templates
    data object ToggleTemplatesExpanded : EditFigureEvent
    data class ApplyTemplate(val template: FigureTemplate) : EditFigureEvent

    // Add-joint mode
    data object ToggleAddJointMode : EditFigureEvent
    data class UpdateNewJointType(val type: SegmentType) : EditFigureEvent

    // Actions
    data object Save : EditFigureEvent
    data object Cancel : EditFigureEvent
}

data class EditFigureState(
    val figure: Figure,
    val selectedJointId: String?,
    val canvasState: CanvasState,
    val figureModificationCount: Long,
    val isNewFigure: Boolean,
    val addJointMode: Boolean,
    val newJointType: SegmentType,
    val templatesExpanded: Boolean = false
) {
    val selectedJoint: Joint?
        get() = selectedJointId?.let { findJointById(figure.root, it) }

    val compiledJoints: List<CompiledJoint>
        get() = figure.compileForEditing()

    val canDeleteSelectedJoint: Boolean
        get() = selectedJoint != null && selectedJoint != figure.root
}

class EditFigureViewModel(
    c: ComponentContext,
    initialFigure: Figure?,
    private val onSave: (Figure) -> Unit,
    private val onCancel: () -> Unit
) : ViewModel<EditFigureEvent, EditFigureState>(c) {

    private val isNewFigure = initialFigure == null

    private val _figure = MutableStateFlow(
        initialFigure?.deepCopy() ?: FigureTemplate.BLANK.createFigure()
    )
    private val _selectedJointId = MutableStateFlow<String?>(null)
    private val _canvasState = MutableStateFlow(CanvasState())
    private val _figureModificationCount = MutableStateFlow(0L)
    private val _templatesExpanded = MutableStateFlow(false)
    private val _addJointMode = MutableStateFlow(false)
    private val _newJointType = MutableStateFlow<SegmentType>(SegmentType.Line)

    private fun incrementModificationCount() {
        _figureModificationCount.value++
    }

    private fun selectJoint(jointId: String?) {
        _selectedJointId.value = jointId
    }

    private fun updateJointLength(length: Float) {
        val jointId = _selectedJointId.value ?: return

        // Joint length is immutable, so we need to replace the joint
        updateJointLengthForJoint(jointId, length)
    }

    private fun updateJointLengthForJoint(jointId: String, length: Float) {
        replaceJoint(jointId) { it.copy(length = length.coerceAtLeast(0f)) }
        incrementModificationCount()
    }

    private fun updateJointAngle(angle: Float) {
        val jointId = _selectedJointId.value ?: return
        replaceJoint(jointId) { it.copy(angle = angle) }
        incrementModificationCount()
    }

    private fun updateJointType(type: SegmentType) {
        val jointId = _selectedJointId.value ?: return

        replaceJoint(jointId) { it.copy(type = type) }
        incrementModificationCount()
    }

    private fun updateJointId(newId: String) {
        val currentId = _selectedJointId.value ?: return
        if (newId == currentId) return

        replaceJoint(currentId) { it.copy(id = newId) }
        _selectedJointId.value = newId
        incrementModificationCount()
    }

    private fun replaceJoint(jointId: String, transform: (Joint) -> Joint) {
        val figure = _figure.value
        val newRoot = replaceJointInTree(figure.root, jointId, transform)
        _figure.value = figure.copy(root = newRoot)
    }

    private fun replaceJointInTree(joint: Joint, targetId: String, transform: (Joint) -> Joint): Joint {
        return if (joint.id == targetId) {
            val transformed = transform(joint)
            // Preserve children from original joint
            Joint(
                id = transformed.id,
                length = transformed.length,
                angle = transformed.angle,
                type = transformed.type,
                children = joint.children
            )
        } else {
            Joint(
                id = joint.id,
                length = joint.length,
                angle = joint.angle,
                type = joint.type,
                children = joint.children.mapTo(mutableListOf()) { child ->
                    replaceJointInTree(child, targetId, transform)
                }
            )
        }
    }

    private fun addChildJoint(parentId: String, type: SegmentType) {
        val parent = findJointById(_figure.value.root, parentId) ?: return

        val newId = generateUniqueJointId(_figure.value)
        val newJoint = Joint(
            id = newId,
            length = 30f,
            angle = 0f,
            type = type
        )

        parent.children += newJoint
        _selectedJointId.value = newId
        incrementModificationCount()
    }

    private fun deleteSelectedJoint() {
        val jointId = _selectedJointId.value ?: return
        val figure = _figure.value

        // Cannot delete root
        if (figure.root.id == jointId) return

        if (removeJoint(figure.root, jointId)) {
            _selectedJointId.value = null
            incrementModificationCount()
        }
    }

    private fun rotateJoint(joint: Joint, newAngle: Float) {
        replaceJoint(joint.id) { it.copy(angle = newAngle) }
        incrementModificationCount()
    }

    private fun moveFigure(newX: Float, newY: Float) {
        val figure = _figure.value
        figure.x = newX
        figure.y = newY
        incrementModificationCount()
    }

    private fun updateFigureName(name: String) {
        _figure.value = _figure.value.copy(name = name)
    }

    private fun applyTemplate(template: FigureTemplate) {
        val currentFigure = _figure.value
        _figure.value = template.createFigure(x = currentFigure.x, y = currentFigure.y)
        _selectedJointId.value = null
        _templatesExpanded.value = false
        incrementModificationCount()
    }

    @Composable
    override fun models(events: Flow<EditFigureEvent>): EditFigureState {
        val figure by _figure.collectAsState()
        val selectedJointId by _selectedJointId.collectAsState()
        val canvasState by _canvasState.collectAsState()
        val modCount by _figureModificationCount.collectAsState()
        val templatesExpanded by _templatesExpanded.collectAsState()
        val addJointMode by _addJointMode.collectAsState()
        val newJointType by _newJointType.collectAsState()

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is EditFigureEvent.SelectJoint -> selectJoint(event.jointId)
                    is EditFigureEvent.UpdateJointLength -> updateJointLength(event.length)
                    is EditFigureEvent.UpdateJointLengthForJoint ->
                        updateJointLengthForJoint(event.jointId, event.length)
                    is EditFigureEvent.UpdateJointAngle -> updateJointAngle(event.angle)
                    is EditFigureEvent.UpdateJointType -> updateJointType(event.type)
                    is EditFigureEvent.UpdateJointId -> updateJointId(event.newId)
                    is EditFigureEvent.AddChildJoint -> addChildJoint(event.parentId, event.type)
                    is EditFigureEvent.DeleteSelectedJoint -> deleteSelectedJoint()
                    is EditFigureEvent.UpdateCanvasState -> _canvasState.value = event.canvasState
                    is EditFigureEvent.RotateJoint -> rotateJoint(event.joint, event.newAngle)
                    is EditFigureEvent.MoveFigure -> moveFigure(event.newX, event.newY)
                    is EditFigureEvent.UpdateFigureName -> updateFigureName(event.name)
                    is EditFigureEvent.ToggleTemplatesExpanded ->
                        _templatesExpanded.value = !_templatesExpanded.value
                    is EditFigureEvent.ApplyTemplate -> applyTemplate(event.template)
                    is EditFigureEvent.ToggleAddJointMode ->
                        _addJointMode.value = !_addJointMode.value
                    is EditFigureEvent.UpdateNewJointType -> _newJointType.value = event.type
                    is EditFigureEvent.Save -> onSave(_figure.value)
                    is EditFigureEvent.Cancel -> onCancel()
                }
            }
        }

        return EditFigureState(
            figure = figure,
            selectedJointId = selectedJointId,
            canvasState = canvasState,
            figureModificationCount = modCount,
            isNewFigure = isNewFigure,
            addJointMode = addJointMode,
            newJointType = newJointType,
            templatesExpanded = templatesExpanded
        )
    }
}
