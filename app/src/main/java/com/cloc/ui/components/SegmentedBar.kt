package com.cloc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cloc.ui.theme.ND
import kotlin.math.roundToInt

/** A progress bar made of separate blocks, like an equaliser. Always show a number next to it. */
@Composable
fun SegmentedBar(
    value: Float,
    max: Float,
    modifier: Modifier = Modifier,
    segments: Int = 20,
    height: Dp = 10.dp,
    fill: Color = ND.colors.textDisplay,
) {
    val empty = ND.colors.segmentEmpty
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, max), 0f..max) },
    ) {
        val gap = 2.dp.toPx()
        val segmentWidth = (size.width - gap * (segments - 1)) / segments
        val filled = ((value / max) * segments).roundToInt().coerceIn(0, segments)
        repeat(segments) { i ->
            drawRect(
                color = if (i < filled) fill else empty,
                topLeft = Offset(i * (segmentWidth + gap), 0f),
                size = Size(segmentWidth, size.height),
            )
        }
    }
}
