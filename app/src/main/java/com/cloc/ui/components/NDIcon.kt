package com.cloc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cloc.R
import com.cloc.ui.theme.ND

/** Thin line icons, drawn in code so they match the Nothing style exactly. */
enum class NDIconType { Play, Pause, Reset, Close, Plus, Minus }

@Composable
fun NDIcon(type: NDIconType, modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = ND.colors.textDisplay) {
    if (type == NDIconType.Reset) {
        // Premade "rotate-ccw" icon from Lucide (res/drawable/ic_reset.xml), tinted to match.
        Image(
            painter = painterResource(R.drawable.ic_reset),
            contentDescription = null,
            modifier = modifier.size(size),
            colorFilter = ColorFilter.tint(color),
        )
        return
    }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        when (type) {
            NDIconType.Play -> {
                val path = Path().apply {
                    moveTo(0.32f * s, 0.22f * s)
                    lineTo(0.80f * s, 0.50f * s)
                    lineTo(0.32f * s, 0.78f * s)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            NDIconType.Pause -> {
                drawLine(color, p(0.36f, 0.24f), p(0.36f, 0.76f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.64f, 0.24f), p(0.64f, 0.76f), stroke.width, StrokeCap.Round)
            }
            NDIconType.Reset -> Unit // drawn from a vector resource, see above
            NDIconType.Close -> {
                drawLine(color, p(0.28f, 0.28f), p(0.72f, 0.72f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.72f, 0.28f), p(0.28f, 0.72f), stroke.width, StrokeCap.Round)
            }
            NDIconType.Plus -> {
                drawLine(color, p(0.5f, 0.24f), p(0.5f, 0.76f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.24f, 0.5f), p(0.76f, 0.5f), stroke.width, StrokeCap.Round)
            }
            NDIconType.Minus -> {
                drawLine(color, p(0.24f, 0.5f), p(0.76f, 0.5f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

/**
 * A round icon button. [filled] = solid white with a black icon (the main action);
 * otherwise a thin outline with a white icon.
 */
@Composable
fun NDIconButton(
    type: NDIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    outlined: Boolean = true,
) {
    val c = ND.colors
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) c.textDisplay else Color.Transparent)
            .then(if (!filled && outlined) Modifier.border(1.dp, c.borderVisible, CircleShape) else Modifier)
            .ndClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        NDIcon(type, size = size * 0.46f, color = if (filled) c.black else c.textDisplay)
    }
}
