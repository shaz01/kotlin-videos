package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.SegmentFrameCanvas
import com.olcayaras.figures.getMockSegmentFrame
import com.olcayaras.vidster.ui.theme.AppTheme
import com.olcayaras.vidster.ui.util.onRightClick
import compose.icons.FeatherIcons
import compose.icons.feathericons.MoreVertical
import org.jetbrains.compose.ui.tooling.preview.Preview


val frameShape = RoundedCornerShape(8.dp)

/**
 * Callbacks for timeline frame actions
 */
data class TimelineFrameActions(
    val onSelect: (Int) -> Unit,
    val onDuplicate: (Int) -> Unit,
    val onInsertBefore: (Int) -> Unit,
    val onInsertAfter: (Int) -> Unit,
    val onDelete: (Int) -> Unit,
    val onEnterSelectionMode: () -> Unit,
    val onToggleSelection: (Int) -> Unit,
    val onReorder: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
) {
    companion object {
        // For preview, etc.
        val Empty = TimelineFrameActions(
            onSelect = {},
            onDuplicate = {},
            onInsertBefore = {},
            onInsertAfter = {},
            onDelete = {},
            onEnterSelectionMode = {},
            onToggleSelection = {}
        )
    }
}

@Composable
fun EditorTimelineRow(
    modifier: Modifier = Modifier,
    frames: List<SegmentFrame>,
    currentFrameIndex: Int,
    viewportSize: IntSize,
    innerPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    selectionMode: Boolean = false,
    selectedFrameIndices: Set<Int> = emptySet(),
    actions: TimelineFrameActions
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier
    ) {
        LazyRow(
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + innerPadding,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + innerPadding,
                top = contentPadding.calculateTopPadding() + innerPadding,
                bottom = contentPadding.calculateBottomPadding() + innerPadding
            )
        ) {
            editorTimelineContent(
                frames = frames,
                currentFrameIndex = currentFrameIndex,
                viewportSize = viewportSize,
                selectionMode = selectionMode,
                selectedFrameIndices = selectedFrameIndices,
                canDelete = frames.size > 1,
                actions = actions
            )
        }
    }
}

@Composable
fun EditorTimelineColumn(
    modifier: Modifier = Modifier,
    frames: List<SegmentFrame>,
    currentFrameIndex: Int,
    viewportSize: IntSize,
    innerPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    selectionMode: Boolean = false,
    selectedFrameIndices: Set<Int> = emptySet(),
    actions: TimelineFrameActions
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + innerPadding,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + innerPadding,
                top = contentPadding.calculateTopPadding() + innerPadding,
                bottom = contentPadding.calculateBottomPadding() + innerPadding
            )
        ) {
            editorTimelineContent(
                frames = frames,
                currentFrameIndex = currentFrameIndex,
                viewportSize = viewportSize,
                selectionMode = selectionMode,
                selectedFrameIndices = selectedFrameIndices,
                canDelete = frames.size > 1,
                actions = actions,
            )
        }
    }
}

fun LazyListScope.editorTimelineContent(
    frames: List<SegmentFrame>,
    currentFrameIndex: Int,
    viewportSize: IntSize = IntSize(1920, 1080),
    selectionMode: Boolean,
    selectedFrameIndices: Set<Int>,
    canDelete: Boolean,
    actions: TimelineFrameActions
) {
    itemsIndexed(frames) { index, frame ->
        EditorTimelineFrame(
            modifier = Modifier.fillMaxWidth(),
            index = index,
            isCurrentFrame = index == currentFrameIndex,
            segmentFrame = frame,
            viewportSize = viewportSize,
            selectionMode = selectionMode,
            isSelectedForBatch = index in selectedFrameIndices,
            canDelete = canDelete,
            actions = actions,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorTimelineFrame(
    modifier: Modifier = Modifier,
    index: Int,
    isCurrentFrame: Boolean = false,
    segmentFrame: SegmentFrame,
    viewportSize: IntSize,
    backgroundColor: Color = Color.White,
    selectionMode: Boolean = false,
    isSelectedForBatch: Boolean = false,
    canDelete: Boolean = true,
    actions: TimelineFrameActions,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { actions.onSelect(index) }
                .onRightClick { showContextMenu = true }
                .background(if (isCurrentFrame) MaterialTheme.colorScheme.surface else Color.Transparent)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox (only shown in selection mode)
            if (selectionMode) {
                Checkbox(
                    checked = isSelectedForBatch,
                    onCheckedChange = { actions.onToggleSelection(index) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Frame content
            Column(modifier = Modifier.weight(1f)) {
                SegmentFrameCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(frameShape)
                        .background(backgroundColor, frameShape)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, frameShape),
                    frame = segmentFrame,
                    viewportSize = viewportSize
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    text = "Frame ${index + 1}"
                )
            }

            // Menu button (only shown when not in selection mode)
            if (!selectionMode) {
                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = FeatherIcons.MoreVertical,
                        contentDescription = "Frame options",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Context menu
        FrameContextMenu(
            expanded = showContextMenu,
            onDismiss = { showContextMenu = false },
            canDelete = canDelete,
            onDuplicate = { actions.onDuplicate(index) },
            onInsertBefore = { actions.onInsertBefore(index) },
            onInsertAfter = { actions.onInsertAfter(index + 1) },
            onDelete = { actions.onDelete(index) },
            onSelect = {
                actions.onEnterSelectionMode()
                actions.onToggleSelection(index)
            },
        )
    }
}

@Preview
@Composable
private fun EditorTimelineFramePreview() {
    MaterialTheme {
        Surface {
            Row {
                EditorTimelineFrame(
                    modifier = Modifier.width(128.dp),
                    index = 0,
                    segmentFrame = getMockSegmentFrame(),
                    viewportSize = IntSize(1920, 1080),
                    backgroundColor = Color.White,
                    actions = TimelineFrameActions.Empty
                )
                EditorTimelineFrame(
                    modifier = Modifier.width(128.dp),
                    index = 1,
                    isCurrentFrame = true,
                    segmentFrame = getMockSegmentFrame(),
                    viewportSize = IntSize(1920, 1080),
                    backgroundColor = Color.White,
                    actions = TimelineFrameActions.Empty
                )
            }
        }
    }
}

@Preview
@Composable
private fun EditorTimelineRowPreview() {
    AppTheme {
        Surface {
            val frames = List(5) { getMockSegmentFrame() }
            EditorTimelineRow(
                frames = frames,
                currentFrameIndex = 2,
                viewportSize = IntSize(1920, 1080),
                innerPadding = 8.dp,
                actions = TimelineFrameActions.Empty
            )
        }
    }
}

@Preview
@Composable
private fun EditorTimelineColumnPreview() {
    AppTheme {
        Surface {
            val frames = List(5) { getMockSegmentFrame() }
            EditorTimelineColumn(
                frames = frames,
                currentFrameIndex = 2,
                viewportSize = IntSize(1920, 1080),
                innerPadding = 8.dp,
                actions = TimelineFrameActions(
                    onSelect = {},
                    onDuplicate = {},
                    onInsertBefore = {},
                    onInsertAfter = {},
                    onDelete = {},
                    onEnterSelectionMode = {},
                    onToggleSelection = {}
                )
            )
        }
    }
}
