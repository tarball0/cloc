package com.cloc.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cloc.data.TimerItem
import com.cloc.data.TimerStore
import com.cloc.ui.components.NDIconButton
import com.cloc.ui.components.NDIconType
import com.cloc.ui.components.NDLabel
import com.cloc.ui.theme.ND
import com.cloc.util.TimeFormat
import kotlin.math.sqrt

/** Trial: red sand. Set to false for white sand. */
private const val RED_SAND = true

/** A centred card on a dark see-through backdrop. Tap outside or the × to close. */
@Composable
fun NDDialogCard(onDismiss: () -> Unit, square: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(ND.space.md)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .then(if (square) Modifier.aspectRatio(1f) else Modifier)
                .background(ND.colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, ND.colors.borderVisible, RoundedCornerShape(16.dp))
                .padding(ND.space.lg),
            content = content,
        )
    }
}

/** The square pop-up with the hourglass, shown when you tap a running timer. */
@Composable
fun SandTimerDialog(timer: TimerItem, onDismiss: () -> Unit) {
    val now = rememberNow(timer.running, periodMs = 100)
    val remaining = timer.remainingAt(now)
    val fraction = if (timer.durationMs > 0) remaining.toFloat() / timer.durationMs else 0f

    NDDialogCard(onDismiss = onDismiss, square = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NDLabel(TimeFormat.durationLabel(timer.durationMs), Modifier.weight(1f))
            NDIconButton(NDIconType.Close, onDismiss, outlined = false, size = 40.dp)
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = ND.space.sm), contentAlignment = Alignment.Center) {
            val white = ND.colors.textDisplay
            val sand = if (RED_SAND) ND.colors.accent else white
            // Sized by the space left over, so it never covers the time or the buttons.
            Canvas(Modifier.fillMaxHeight().aspectRatio(0.62f, matchHeightConstraintsFirst = true)) {
                drawHourglass(fraction, white, sand)
            }
        }
        Text(
            TimeFormat.countdown(remaining),
            style = ND.type.numberLg,
            color = ND.colors.textDisplay,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(ND.space.md))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ND.space.lg, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NDIconButton(NDIconType.Reset, { TimerStore.reset(timer.id) }, size = 56.dp)
            NDIconButton(
                if (timer.running) NDIconType.Pause else NDIconType.Play,
                { TimerStore.toggle(timer.id) },
                size = 76.dp,
                filled = true,
            )
            Spacer(Modifier.height(1.dp).widthIn(min = 56.dp)) // balances the reset button
        }
    }
}

/**
 * Hourglass: two triangles meeting at a narrow neck.
 * The top is solid white down to a level that drops as time runs out; the bottom fills up.
 * Levels use the triangle's area, so the white *amount* moves at a steady rate.
 */
private fun DrawScope.drawHourglass(remainingFraction: Float, white: Color, sand: Color) {
    val w = size.width
    val h = size.height
    val neckY = h / 2
    val neck = w * 0.06f
    val half = neckY

    val top = Path().apply {
        moveTo(0f, 0f); lineTo(w, 0f)
        lineTo(w / 2 + neck, neckY); lineTo(w / 2 - neck, neckY); close()
    }
    val bottom = Path().apply {
        moveTo(w / 2 - neck, neckY); lineTo(w / 2 + neck, neckY)
        lineTo(w, h); lineTo(0f, h); close()
    }

    val r = remainingFraction.coerceIn(0f, 1f)
    // Top: sand sits on the neck; its height is √r of the bulb (a triangle's area grows with height²).
    val topLevel = neckY - half * sqrt(r)
    clipPath(top) { drawRect(sand, topLeft = Offset(0f, topLevel), size = androidx.compose.ui.geometry.Size(w, neckY - topLevel)) }
    // Bottom: sand piles from the base; filled height = 1 − √r of the bulb.
    val bottomLevel = h - half * (1 - sqrt(r))
    clipPath(bottom) { drawRect(sand, topLeft = Offset(0f, bottomLevel), size = androidx.compose.ui.geometry.Size(w, h - bottomLevel)) }

    val stroke = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
    drawPath(top, white, style = stroke)
    drawPath(bottom, white, style = stroke)
}
