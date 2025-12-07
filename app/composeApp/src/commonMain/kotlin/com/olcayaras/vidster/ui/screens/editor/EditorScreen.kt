package com.olcayaras.vidster.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.olcayaras.figures.*
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
    Box(Modifier.fillMaxSize().background(Color.White)) {
        // Canvas layer (background) - Interactive infinite canvas
        InfiniteCanvas(
            modifier = Modifier.fillMaxSize(),
            figures = model.selectedFigures,
            canvasState = model.canvasState,
            figureModificationCount = model.figureModificationCount,
            rotationAllowed = true,
            onCanvasStateChange = { take(EditorEvent.UpdateCanvasState(it)) },
            onJointAngleChanged = { figure, joint, angle ->
                take(EditorEvent.UpdateJointAngle(figure, joint, angle))
            },
            onFigureMoved = { figure, newX, newY ->
                take(EditorEvent.MoveFigure(figure, newX, newY))
            }
        )

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
                frames = model.segmentFrames,
                onClick = { segmentFrame ->
                    val index = model.segmentFrames.indexOf(segmentFrame)
                    if (index >= 0) {
                        take(EditorEvent.SelectFrame(index))
                    }
                },
                selectedFrame = model.selectedSegmentFrame,
                screenSize = model.screenSize
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
                    FilledTonalIconButton(onClick = { take(EditorEvent.PlayAnimation) }) {
                        Icon(FeatherIcons.Play, contentDescription = "Play Animation")
                    }
                    VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(FeatherIcons.Type, contentDescription = null)
                    }
                    IconButton(onClick = { take(EditorEvent.AddFrame) }) {
                        Icon(FeatherIcons.Plus, contentDescription = "Add Frame")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(FeatherIcons.Loader, contentDescription = null)
                    }
                }
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
    val frames = listOf(
        FigureFrame(
            figures = listOf(getMockFigure(x = 400f, y = 200f)),
            viewport = Viewport()
        )
    )
    EditorScreen(
        model = EditorState(
            frames = frames,
            selectedFrameIndex = 0,
            canvasState = CanvasState()
        ),
        take = {}
    )
}
