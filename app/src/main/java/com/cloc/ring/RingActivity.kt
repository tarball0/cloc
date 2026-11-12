package com.cloc.ring

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cloc.ui.components.NDButton
import com.cloc.ui.components.NDButtonStyle
import com.cloc.ui.components.NDLabel
import com.cloc.ui.components.TimeText
import com.cloc.ui.theme.ND
import com.cloc.ui.theme.NothingTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * The screen that appears over the lock screen while an alarm or timer rings.
 * No snooze for batch alarms: just a big OK and a small I'M UP.
 */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        // Back does nothing: you have to choose a button.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        setContent {
            NothingTheme {
                val info by RingService.current.collectAsState()
                LaunchedEffect(info) { if (info == null) finish() }
                info?.let { RingScreen(it) { action -> RingService.act(this, action) } }
            }
        }
    }
}

@Composable
private fun RingScreen(info: RingInfo, onAction: (String) -> Unit) {
    val is24h = DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }

    val title = when (info.kind) {
        RingKind.TIMER -> "Timer done"
        RingKind.ALARM -> info.label.ifBlank { "Alarm" }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ND.colors.black)
            .systemBarsPadding()
            .padding(ND.space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        NDLabel(title, color = ND.colors.textDisplay)
        if (info.kind == RingKind.ALARM && info.isBatch) {
            Spacer(Modifier.height(ND.space.xs))
            NDLabel("${info.position} / ${info.total}")
        }
        Spacer(Modifier.height(ND.space.xl))

        // The alarm-clock logo, drawn around the time.
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            val white = ND.colors.textDisplay
            Canvas(Modifier.fillMaxSize()) { drawAlarmClock(white) }
            Box(Modifier.padding(top = 30.dp)) {
                TimeText(now.hour * 60 + now.minute, is24h, ND.type.numberLg)
            }
        }

        Spacer(Modifier.weight(1f))

        NDButton(
            "OK",
            { onAction(RingService.ACTION_OK) },
            Modifier.fillMaxWidth().height(72.dp),
        )
        Spacer(Modifier.height(ND.space.md))
        when {
            info.kind == RingKind.TIMER -> Spacer(Modifier.height(48.dp))
            info.isBatch -> NDButton("I'm up", { onAction(RingService.ACTION_IM_UP) }, style = NDButtonStyle.Ghost)
            else -> NDButton("Snooze 10 min", { onAction(RingService.ACTION_SNOOZE) }, style = NDButtonStyle.Secondary)
        }
    }
}

/** A thin ring with two bells on top: an outline alarm clock. */
private fun DrawScope.drawAlarmClock(white: Color) {
    val stroke = 2.dp.toPx()
    val cx = size.width / 2
    val cy = size.height * 0.56f
    val r = size.minDimension * 0.40f
    drawCircle(white, r, Offset(cx, cy), style = Stroke(stroke))

    // Bells: half circles sitting on the ring at ±42° from the top, facing outwards.
    val bellR = r * 0.24f
    listOf(-42.0, 42.0).forEach { deg ->
        val a = Math.toRadians(deg)
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        val center = Offset(cx + dx * (r + stroke * 3), cy + dy * (r + stroke * 3))
        val facing = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        drawArc(
            white, startAngle = facing - 90f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(center.x - bellR, center.y - bellR), size = Size(bellR * 2, bellR * 2),
        )
    }

    // Small hammer post on top.
    drawLine(white, Offset(cx, cy - r), Offset(cx, cy - r - r * 0.12f), stroke, StrokeCap.Round)
    drawLine(white, Offset(cx - r * 0.08f, cy - r - r * 0.12f), Offset(cx + r * 0.08f, cy - r - r * 0.12f), stroke, StrokeCap.Round)
}
