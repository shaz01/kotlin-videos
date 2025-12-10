package com.olcayaras.vidster.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.olcayaras.figures.*
import com.olcayaras.vidster.ui.screens.editor.composables.EditorSheetContainer
import com.olcayaras.vidster.ui.screens.editor.composables.EditorTimelineColumn
import com.olcayaras.vidster.ui.screens.editor.composables.EditorToolbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Loader
import compose.icons.feathericons.Play
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Type
import io.github.aakira.napier.Napier
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/**
 * Calculates the centered offset for the canvas based on viewport and available space.
 * @return CanvasState containing the calculated offset and scale, or null if the canvas size is zero
 */
private fun calculateCenteredOffset(
    canvasSize: IntSize,
    density: Density,
    left: Int = 0,
    top: Int = 0,
    right: Int = canvasSize.width,
    bottom: Int = canvasSize.height,
    viewport: Viewport,
    viewportSize: IntSize,
): CanvasState? {
    if (canvasSize == IntSize.Zero) return null

    val eightDpInPx = with(density) { 8.dp.toPx() }

    val safeLeft = left + eightDpInPx
    val safeTop = top + eightDpInPx
    val safeRight = right - eightDpInPx
    val safeBottom = bottom - eightDpInPx

    val safeWidth = safeRight - safeLeft
    val safeHeight = safeBottom - safeTop
    val viewportWidth = viewportSize.width
    val viewportHeight = viewportSize.height
    if (safeWidth <= 0 || safeHeight <= 0) return null
    if (viewportWidth <= 0 || viewportHeight <= 0) return null

    val scale = minOf(safeWidth / viewportWidth, safeHeight / viewportHeight)

    val equalizingPaddingX = (safeWidth - viewportWidth * scale) / 2
    val equalizingPaddingY = (safeHeight - viewportHeight * scale) / 2

    return CanvasState(
        offsetX = safeLeft + equalizingPaddingX - viewport.leftX * scale,
        offsetY = safeTop + equalizingPaddingY - viewport.topY * scale,
        scale = scale
    )
}

/**
 * Main editor screen composable that displays the animation editor interface.
 * Contains an infinite canvas for figure manipulation, timeline, toolbar, and properties panel.
 *
 * @param model The current state of the editor
 * @param take Callback to handle editor events
 */
@Composable
fun EditorScreen(
    model: EditorState,
    take: (EditorEvent) -> Unit
) {
    val density = LocalDensity.current
    // Measurement state for overlay sizes
    var timelineWidth by remember { mutableIntStateOf(0) }
    var toolbarHeight by remember { mutableIntStateOf(0) }

    var propertiesPanelLeft by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun resetViewportToCenter() {
        calculateCenteredOffset(
            canvasSize = canvasSize,
            density = density,
            left = timelineWidth,
            top = toolbarHeight,
            right = propertiesPanelLeft,
            viewport = model.selectedFrame.viewport,
            viewportSize = model.screenSize
        )?.let {
            take(EditorEvent.UpdateCanvasState(it))
        } ?: Napier.e { "Failed to calculate centered offset" }
    }

    // Set initial offset when measurements are available
    var hasInitializedOffset by remember { mutableStateOf(false) }
    LaunchedEffect(timelineWidth, toolbarHeight, propertiesPanelLeft, canvasSize) {
        if (canvasSize != IntSize.Zero && !hasInitializedOffset) {
            @Suppress("AssignedValueIsNeverRead") // ide bug
            hasInitializedOffset = true
            resetViewportToCenter()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { canvasSize = it.size }
    ) {
        // Canvas layer (background) - Interactive infinite canvas
        InfiniteCanvas(
            modifier = Modifier.fillMaxSize(),
            figures = model.selectedFigures,
            canvasState = model.canvasState,
            viewport = model.selectedFrame.viewport,
            screenSize = model.screenSize,
            figureModificationCount = model.figureModificationCount,
            rotationAllowed = true,
            onCanvasStateChange = { take(EditorEvent.UpdateCanvasState(it)) },
            onViewportChanged = { take(EditorEvent.UpdateViewport(it)) },
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
                    .padding(16.dp)
                    .onGloballyPositioned {
                        timelineWidth = it.boundsInParent().right.roundToInt()
                    },
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
                        .onGloballyPositioned {
                            toolbarHeight = it.boundsInParent().bottom.roundToInt()
                        }
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
                    VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    IconButton(onClick = ::resetViewportToCenter) {
                        Icon(FeatherIcons.Crosshair, contentDescription = "Reset View")
                    }
                }
            }

            // Right: Properties panel
            EditorSheetContainer(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .onGloballyPositioned {
                        propertiesPanelLeft = it.boundsInParent().left.roundToInt()
                    }
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
