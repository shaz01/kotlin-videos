package com.olcayaras.lib.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.olcayaras.lib.currentDuration
import com.olcayaras.lib.videobuilders.SequenceScope
import com.olcayaras.lib.videobuilders.SequenceWithTTSScope
import kotlin.time.Duration.Companion.seconds

enum class SubtitleStyle {
    BASIC,
    WORD_HIGHLIGHT,
    CHAR_HIGHLIGHT
}

@Composable
fun SequenceScope.AnimatedSubtitle(
    subtitle: Subtitle,
    style: SubtitleStyle = SubtitleStyle.BASIC,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = Color.White,
    highlightColor: Color = Color.Yellow,
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    cornerRadius: Dp = 8.dp,
    padding: Dp = 12.dp,
    maxLines: Int = 2,
    textAlign: TextAlign = TextAlign.Center,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Medium
) {
    val finalTextStyle = textStyle.copy(
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .padding(padding)
    ) {
        when (style) {
            SubtitleStyle.BASIC -> BasicSubtitle(
                subtitle = subtitle,
                textStyle = finalTextStyle,
                maxLines = maxLines
            )

            SubtitleStyle.WORD_HIGHLIGHT -> WordHighlightSubtitle(
                subtitle = subtitle,
                textStyle = finalTextStyle,
                highlightColor = highlightColor,
                maxLines = maxLines
            )

            SubtitleStyle.CHAR_HIGHLIGHT -> CharHighlightSubtitle(
                subtitle = subtitle,
                textStyle = finalTextStyle,
                highlightColor = highlightColor,
                maxLines = maxLines
            )
        }
    }
}

@Composable
fun BasicSubtitle(
    subtitle: Subtitle,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = 2
) {
    Text(
        text = subtitle.text,
        modifier = modifier.zIndex(100f),
        style = textStyle,
        maxLines = maxLines
    )
}

@Composable
fun SequenceScope.WordHighlightSubtitle(
    subtitle: Subtitle,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    highlightColor: Color = Color.Yellow,
    maxLines: Int = 2
) {
    // Find the current word being spoken
    val highlightedRange = subtitle.wordTimings
        .firstOrNull { wordTiming ->
            currentDuration() in (wordTiming.startTime)..(wordTiming.endTime)
        }
        ?.let { it.startIndex..it.endIndex } // the range to highlight

    val annotatedString = buildAnnotatedString {
        subtitle.chars.forEachIndexed { index, char ->
            val isHighlighted = highlightedRange?.contains(index) ?: false
            withStyle(
                style = SpanStyle(
                    color = if (isHighlighted) highlightColor else textStyle.color,
                    fontWeight = if (isHighlighted) FontWeight.Bold else textStyle.fontWeight
                )
            ) {
                append(char)
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.zIndex(100f),
        style = textStyle,
        maxLines = maxLines
    )
}

@Composable
fun SequenceScope.CharHighlightSubtitle(
    subtitle: Subtitle,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    highlightColor: Color = Color.Yellow,
    maxLines: Int = 2
) {
    val currentTime = currentDuration()
    val relativeTime = currentTime - subtitle.startTime

    val annotatedString = buildAnnotatedString {
        subtitle.chars.forEachIndexed { index, char ->
            val charStartTime = subtitle.charStarts[index].seconds
            val charEndTime = subtitle.charEnds[index].seconds

            // Adjust times relative to subtitle start
            val adjustedStartTime = charStartTime - subtitle.charStarts[0].seconds
            val adjustedEndTime = charEndTime - subtitle.charStarts[0].seconds

            val isCurrentChar = relativeTime in adjustedStartTime..adjustedEndTime
            val hasBeenSpoken = relativeTime > adjustedEndTime

            withStyle(
                style = SpanStyle(
                    color = when {
                        isCurrentChar -> highlightColor
                        hasBeenSpoken -> textStyle.color.copy(alpha = 0.7f)
                        else -> textStyle.color.copy(alpha = 0.4f)
                    },
                    fontWeight = if (isCurrentChar) FontWeight.Bold else textStyle.fontWeight
                )
            ) {
                append(char)
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.zIndex(100f),
        style = textStyle,
        maxLines = maxLines
    )
}

@Composable
fun SequenceScope.SubtitleContainer(
    subtitle: Subtitle?,
    style: SubtitleStyle = SubtitleStyle.BASIC,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = Color.White,
    highlightColor: Color = Color.Yellow,
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    cornerRadius: Dp = 8.dp,
    padding: Dp = 12.dp,
    maxLines: Int = 2,
    textAlign: TextAlign = TextAlign.Center,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Medium
) {
    subtitle?.let {
        AnimatedSubtitle(
            subtitle = it,
            style = style,
            modifier = modifier,
            textStyle = textStyle,
            textColor = textColor,
            highlightColor = highlightColor,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            padding = padding,
            maxLines = maxLines,
            textAlign = textAlign,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun SequenceWithTTSScope.SubtitleContainer(
    style: SubtitleStyle = SubtitleStyle.BASIC,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = Color.White,
    highlightColor: Color = Color.Yellow,
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    cornerRadius: Dp = 8.dp,
    padding: Dp = 12.dp,
    maxLines: Int = 2,
    textAlign: TextAlign = TextAlign.Center,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Medium
) {
    getSubtitle()?.let {
        SubtitleContainer(
            subtitle = it,
            style = style,
            modifier = modifier,
            textStyle = textStyle,
            textColor = textColor,
            highlightColor = highlightColor,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            padding = padding,
            maxLines = maxLines,
            textAlign = textAlign,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}