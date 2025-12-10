package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.SegmentFrameCanvas
import com.olcayaras.figures.getMockSegmentFrame
import com.olcayaras.vidster.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val innerRadius = RectangleShape
private val outerRadius = RectangleShape

@Composable
fun EditorTimelineRow(
    modifier: Modifier = Modifier,
    frames: List<SegmentFrame>,
    onClick: (SegmentFrame) -> Unit,
    selectedFrame: SegmentFrame?,
    screenSize: IntSize = IntSize(1920, 1080),
    innerPadding: Dp = 0.dp
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier.clip(outerRadius)
    ) {
        LazyRow(contentPadding = PaddingValues(innerPadding)) {
            editorTimelineContent(
                frames = frames,
                onClick = onClick,
                selectedFrame = selectedFrame,
                screenSize = screenSize
            )
        }
    }
}

@Composable
fun EditorTimelineColumn(
    modifier: Modifier = Modifier,
    frames: List<SegmentFrame>,
    onClick: (SegmentFrame) -> Unit,
    selectedFrame: SegmentFrame?,
    screenSize: IntSize = IntSize(1920, 1080),
    innerPadding: Dp = 0.dp
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier.clip(outerRadius)
    ) {
        LazyColumn(contentPadding = PaddingValues(innerPadding)) {
            editorTimelineContent(
                frames = frames,
                onClick = onClick,
                selectedFrame = selectedFrame,
                screenSize = screenSize
            )
        }
    }
}



fun LazyListScope.editorTimelineContent(
    frames: List<SegmentFrame>,
    onClick: (SegmentFrame) -> Unit,
    selectedFrame: SegmentFrame? = null,
    screenSize: IntSize = IntSize(1920, 1080)
) {
    itemsIndexed(frames) { index, frame ->
        EditorTimelineFrame(
            modifier = Modifier.fillMaxWidth(),
            selected = frame === selectedFrame,
            onClick = { onClick(frame) },
            segmentFrame = frame,
            viewportSize = screenSize,
            text = "Frame ${index + 1}"
        )
    }
}

@Composable
private fun EditorTimelineFrame(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    segmentFrame: SegmentFrame,
    viewportSize: IntSize = IntSize(1920, 1080),
    backgroundColor: Color = Color.White,
    text: String
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .clip(innerRadius)
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .padding(8.dp)
    ) {
        SegmentFrameCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
            frame = segmentFrame,
            viewportSize = viewportSize
        )
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            textAlign = TextAlign.Center,
            text = text
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
                    onClick = {},
                    segmentFrame = getMockSegmentFrame(),
                    viewportSize = IntSize(1920, 1080),
                    backgroundColor = Color.White,
                    text = "Frame 2"
                )
                EditorTimelineFrame(
                    modifier = Modifier.width(128.dp),
                    onClick = {},
                    selected = true,
                    segmentFrame = getMockSegmentFrame(),
                    viewportSize = IntSize(1920, 1080),
                    backgroundColor = Color.White,
                    text = "Selected frame"
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
                modifier = Modifier.fillMaxWidth(),
                frames = frames,
                onClick = {},
                selectedFrame = frames[2],
                innerPadding = 8.dp
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
//                modifier = Modifier.fillMaxHeight(),
                frames = frames,
                onClick = {},
                selectedFrame = frames[2],
                innerPadding = 8.dp
            )
        }
    }
}
