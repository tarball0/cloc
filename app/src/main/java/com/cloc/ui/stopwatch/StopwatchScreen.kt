package com.cloc.ui.stopwatch

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.cloc.data.StopwatchStore
import com.cloc.ui.components.NDButton
import com.cloc.ui.components.NDButtonStyle
import com.cloc.ui.components.NDDivider
import com.cloc.ui.components.NDLabel
import com.cloc.ui.theme.ND
import com.cloc.ui.theme.ndTransition
import com.cloc.util.TimeFormat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Stopwatch tab: analog dial (seconds round the edge, hours in the small dial) with the digital time inside. */
@Composable
fun StopwatchScreen() {
    val state by StopwatchStore.state.collectAsState()
    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val view = LocalView.current

    // While running, update on every screen frame (about 60–120 times a second) for a smooth sweep.
    LaunchedEffect(state.running) {
        nowElapsed = SystemClock.elapsedRealtime()
        while (state.running) {
            withFrameNanos { nowElapsed = SystemClock.elapsedRealtime() }
        }
    }
    val elapsed = state.elapsedAt(nowElapsed)

    val laps = state.laps
    // Centred while there are no laps; once laps appear the dial glides up to make room for the list.
    val topWeight by animateFloatAsState(if (laps.isEmpty()) 1f else 0.12f, ndTransition(), label = "swTop")

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = ND.space.md)) {
        Spacer(Modifier.weight(topWeight))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val dial = min(maxWidth.value, 320f).dp
            val white = ND.colors.textDisplay
            val red = ND.colors.accent
            Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawStopwatch(elapsed, white, red) }
                Text(
                    TimeFormat.stopwatch(elapsed),
                    style = ND.type.numberMd,
                    color = white,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = dial * 0.2f),
                )
            }
        }

        Spacer(Modifier.height(ND.space.xl))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ND.space.sm, Alignment.CenterHorizontally)) {
            when {
                state.running -> NDButton("Lap", { StopwatchStore.lap() }, Modifier.width(140.dp), NDButtonStyle.Secondary)
                elapsed > 0 -> NDButton("Reset", { StopwatchStore.reset() }, Modifier.width(140.dp), NDButtonStyle.Secondary)
                else -> NDButton("Lap", {}, Modifier.width(140.dp), NDButtonStyle.Secondary, enabled = false)
            }
            NDButton(
                if (state.running) "Stop" else "Start",
                {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    if (state.running) StopwatchStore.stop() else StopwatchStore.start()
                },
                Modifier.width(140.dp),
            )
        }

        Spacer(Modifier.height(ND.space.lg))

        // Laps, newest first. Each row: number, this lap's time, total time.
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            val rows = laps.mapIndexed { i, total -> Triple(i + 1, total - (laps.getOrNull(i - 1) ?: 0L), total) }.reversed()
            itemsIndexed(rows, key = { _, r -> r.first }) { idx, (n, split, total) ->
                if (idx > 0) NDDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    NDLabel("Lap %02d".format(n), Modifier.weight(1f))
                    Text(TimeFormat.stopwatch(split), style = ND.type.body, color = ND.colors.textDisplay, modifier = Modifier.weight(1f))
                    Text(TimeFormat.stopwatch(total), style = ND.type.body, color = ND.colors.textPrimary)
                }
            }
        }
    }
}

private fun DrawScope.drawStopwatch(elapsedMs: Long, white: Color, red: Color) {
    val c = center
    val r = size.minDimension / 2 - 4.dp.toPx()

    fun polar(radius: Float, deg: Double, around: Offset = c): Offset {
        val a = Math.toRadians(deg - 90)
        return Offset(around.x + radius * cos(a).toFloat(), around.y + radius * sin(a).toFloat())
    }

    // Outer ring: 60 second ticks, every fifth one longer and brighter.
    for (i in 0 until 60) {
        val major = i % 5 == 0
        val len = if (major) 12.dp.toPx() else 6.dp.toPx()
        drawLine(
            white.copy(alpha = if (major) 1f else 0.35f),
            polar(r, i * 6.0), polar(r - len, i * 6.0),
            strokeWidth = if (major) 1.5.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Faint arc showing progress through the current hour (minutes).
    val minuteFraction = (elapsedMs % 3_600_000L) / 3_600_000f
    val arcR = r - 22.dp.toPx()
    drawArc(
        white.copy(alpha = 0.25f), startAngle = -90f, sweepAngle = 360f * minuteFraction, useCenter = false,
        topLeft = Offset(c.x - arcR, c.y - arcR), size = Size(arcR * 2, arcR * 2),
        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
    )

    // Small inner dial for hours (12 positions).
    val sub = Offset(c.x, c.y - r * 0.38f)
    val subR = r * 0.2f
    for (i in 0 until 12) drawCircle(white.copy(alpha = 0.6f), 1.5.dp.toPx(), polar(subR, i * 30.0, sub))
    val hours = elapsedMs / 3_600_000.0
    drawLine(white, sub, polar(subR * 0.8f, (hours % 12) * 30.0, sub), 1.5.dp.toPx(), StrokeCap.Round)
    drawCircle(white, 2.5.dp.toPx(), sub)

    // Seconds hand: one full turn per minute, sweeping smoothly.
    val secDeg = (elapsedMs % 60_000L) / 60_000.0 * 360.0
    drawLine(red, polar(r * 0.12f, secDeg + 180), polar(r - 4.dp.toPx(), secDeg), 1.5.dp.toPx(), StrokeCap.Round)
    drawCircle(red, 4.dp.toPx(), c)
    drawCircle(Color.Black, 1.5.dp.toPx(), c)
}
