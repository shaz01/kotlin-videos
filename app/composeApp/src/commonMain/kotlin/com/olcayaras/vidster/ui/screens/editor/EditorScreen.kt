package com.olcayaras.vidster.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.SegmentFrameCanvas
import com.olcayaras.figures.getMockSegmentFrame
import com.olcayaras.vidster.ui.screens.editor.composables.EditorSheetContainer
import com.olcayaras.vidster.ui.screens.editor.composables.EditorTimelineColumn
import com.olcayaras.vidster.ui.screens.editor.composables.EditorToolbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.Loader
import compose.icons.feathericons.Play
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Type
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
fun EditorScreen(
    model: EditorState,
    take: (EditorEvent) -> Unit
) {
    EditorScreen(
        frames = model.frames,
        onClick = { take(EditorEvent.SelectFrame(it)) },
        selectedFrame = model.selectedFrame,
        screenSize = model.screenSize
    )
}

@Composable
fun EditorScreen(
    frames: List<SegmentFrame>,
    onClick: (SegmentFrame) -> Unit,
    selectedFrame: SegmentFrame?,
    screenSize: IntSize = IntSize(1920, 1080)
) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        // Canvas layer (background)
        val frame = selectedFrame ?: frames.firstOrNull()
        if (frame != null) {
            SegmentFrameCanvas(
                modifier = Modifier.fillMaxSize(),
                screenSize = IntSize(1920, 1080),
                frame = frame
            )
        }

        // UI overlay layer
        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            // Left: Timeline
            EditorTimelineColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(128.dp)
                    .padding(16.dp),
                frames = frames,
                onClick = onClick,
                selectedFrame = selectedFrame,
                screenSize = screenSize
            )

            // Center: Main content area with toolbar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Toolbar at top
                EditorToolbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    FilledTonalIconButton(onClick = { /*TODO*/ }) {
                        Icon(FeatherIcons.Play, contentDescription = null)
                    }
                    VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(FeatherIcons.Type, contentDescription = null)
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(FeatherIcons.Plus, contentDescription = null)
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(FeatherIcons.Loader, contentDescription = null)
                    }
                }

                // Rest of space for canvas
                Spacer(Modifier.weight(1f))
            }

            // Right: Properties panel
            EditorSheetContainer(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text("Properties", style = MaterialTheme.typography.titleMedium)
                // Properties content will go here
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 360, showBackground = true)
@Composable
fun EditorScreenPreview() {
    val selected = getMockSegmentFrame("selected")
    val frames = listOf(selected) + List(10) { getMockSegmentFrame() }
    EditorScreen(
        frames = frames,
        onClick = {},
        selectedFrame = selected
    )
}
