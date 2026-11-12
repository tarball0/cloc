package com.cloc.ui.clock

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cloc.ui.components.NDLabel
import com.cloc.ui.components.TimeText
import com.cloc.ui.theme.ND
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Clock tab: a minimal analog dial, the digital time under it, then the date. */
@Composable
fun ClockScreen() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Wake up exactly on each new second, so the seconds hand ticks in step with the real clock.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }

    val is24h = DateFormat.is24HourFormat(context)
    val dateText = now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())).uppercase()

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val dial = min(maxWidth.value * 0.78f, 340f).dp
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val red = ND.colors.accent
            val white = ND.colors.textDisplay
            Canvas(Modifier.size(dial)) { drawDial(now, white, red) }
            Spacer(Modifier.height(ND.space.xxl))
            TimeText(now.hour * 60 + now.minute, is24h, ND.type.numberXl)
            Spacer(Modifier.height(ND.space.sm))
            NDLabel(dateText)
        }
    }
}

/** Trial: a thin solid ring around the dial. Set to false to go back to the 12 dots. */
private const val DIAL_RING = true

private fun DrawScope.drawDial(now: LocalDateTime, white: Color, red: Color) {
    val c = center
    val r = size.minDimension / 2

    if (DIAL_RING) {
        drawCircle(white, radius = r * 0.97f, center = c, style = Stroke(1.5.dp.toPx()))
    } else {
        // 12 dots instead of numbers: the dot-matrix motif.
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val p = Offset(c.x + (r * 0.94f) * cos(a).toFloat(), c.y + (r * 0.94f) * sin(a).toFloat())
            drawCircle(white, radius = if (i % 3 == 0) 3.5.dp.toPx() else 2.dp.toPx(), center = p)
        }
    }

    fun hand(angleDeg: Double, length: Float, tail: Float, width: Float, color: Color) {
        val a = Math.toRadians(angleDeg - 90)
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        drawLine(
            color,
            start = Offset(c.x - dx * tail, c.y - dy * tail),
            end = Offset(c.x + dx * length, c.y + dy * length),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }

    val hourAngle = (now.hour % 12) * 30.0 + now.minute * 0.5
    val minuteAngle = now.minute * 6.0 + now.second * 0.1
    val secondAngle = now.second * 6.0

    hand(hourAngle, r * 0.50f, 0f, 5.dp.toPx(), white)
    hand(minuteAngle, r * 0.78f, 0f, 3.dp.toPx(), white)
    hand(secondAngle, r * 0.86f, r * 0.14f, 1.5.dp.toPx(), red)
    drawCircle(red, radius = 4.dp.toPx(), center = c)
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = c)
}
