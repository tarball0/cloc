package com.cloc.ui.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cloc.data.Alarm
import com.cloc.data.AlarmKind
import com.cloc.data.AlarmStore
import com.cloc.data.AppEvents
import com.cloc.ui.components.NDButton
import com.cloc.ui.components.NDButtonStyle
import com.cloc.ui.components.NDLabel
import com.cloc.ui.components.NDSwitch
import com.cloc.ui.components.NDTag
import com.cloc.ui.components.TimeText
import com.cloc.ui.components.daysLabel
import com.cloc.ui.components.ndClickable
import com.cloc.ui.theme.ND
import java.time.LocalDate

/** Alarms tab: the list of alarm cards. Tapping a card or + NEW opens [AlarmEditor]. */
@Composable
fun AlarmScreen() {
    val alarms by AlarmStore.alarms.collectAsState()
    val is24h = DateFormat.is24HourFormat(LocalContext.current)

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = ND.space.md)) {
        Row(
            Modifier.fillMaxWidth().padding(top = ND.space.xl, bottom = ND.space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alarms", style = ND.type.heading, color = ND.colors.textDisplay, modifier = Modifier.weight(1f))
            NDButton("+ New", { AlarmEditorState.open(null) })
        }

        PermissionWarnings()

        if (alarms.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                NDLabel("No alarms yet")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(ND.space.sm),
            ) {
                items(alarms, key = { it.id }) { alarm -> AlarmCard(alarm, is24h) }
                item { Spacer(Modifier.height(ND.space.md)) }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, is24h: Boolean) {
    val c = ND.colors
    val skippedToday = alarm.skipDate == LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(16.dp))
            .ndClickable { AlarmEditorState.open(alarm) }
            .padding(ND.space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (alarm.enabled) 1f else 0.4f)) {
                if (alarm.isBatch) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NDTag(kindLabel(alarm.kind))
                        if (alarm.label.isNotBlank()) {
                            Spacer(Modifier.width(ND.space.sm))
                            NDLabel(alarm.label, color = c.textDisplay)
                        }
                    }
                    Spacer(Modifier.height(ND.space.sm))
                    Row(verticalAlignment = Alignment.Bottom) {
                        TimeText(alarm.times.first(), is24h, ND.type.numberMd)
                        Text("  →  ", style = ND.type.body, color = c.textDisplay, modifier = Modifier.padding(bottom = 2.dp))
                        TimeText(alarm.times.last(), is24h, ND.type.numberMd)
                    }
                } else {
                    if (alarm.label.isNotBlank()) {
                        NDLabel(alarm.label, color = c.textDisplay)
                        Spacer(Modifier.height(ND.space.xs))
                    }
                    TimeText(alarm.times.first(), is24h, ND.type.numberLg)
                }
            }
            NDSwitch(alarm.enabled, { AlarmStore.setEnabled(alarm.id, it) })
        }

        if (alarm.isBatch) {
            Spacer(Modifier.height(ND.space.md))
            BatchTimeline(alarm.times, Modifier.alpha(if (alarm.enabled) 1f else 0.4f))
        }

        Spacer(Modifier.height(ND.space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NDLabel(daysLabel(alarm.days))
            if (alarm.isBatch) {
                NDLabel("  ·  ${alarm.times.size} alarms")
            }
            if (alarm.times.last() >= 1440) NDLabel("  ·  ends next day")
            Spacer(Modifier.weight(1f))
            if (skippedToday && alarm.enabled) NDLabel("[Skipped today]", color = c.textDisplay)
        }
    }
}

/** A line with a dot for each alarm in the batch, spaced by real time. */
@Composable
private fun BatchTimeline(times: List<Int>, modifier: Modifier = Modifier) {
    val white = ND.colors.textDisplay
    val line = ND.colors.borderVisible
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val r = 3.dp.toPx()
        val y = size.height / 2
        drawLine(line, Offset(r, y), Offset(size.width - r, y), 1.dp.toPx())
        val first = times.first()
        val span = (times.last() - first).coerceAtLeast(1)
        times.forEach { t ->
            val x = r + (size.width - 2 * r) * (t - first) / span
            drawCircle(white, r, Offset(x, y))
        }
    }
}

/**
 * Red warning rows when Android settings would stop alarms from showing.
 * Rechecked every time the app comes back to the front.
 */
@Composable
private fun PermissionWarnings() {
    val ctx = LocalContext.current
    val resumed by AppEvents.resumed.collectAsState()

    val notificationsOk = remember(resumed) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val fullScreenOk = remember(resumed) {
        Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val exactOk = remember(resumed) { ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }

    val appSettings = {
        ctx.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) appSettings()
        AppEvents.resumed.value++
    }

    @Composable
    fun Warning(text: String, onFix: () -> Unit) {
        Row(Modifier.fillMaxWidth().padding(bottom = ND.space.sm), verticalAlignment = Alignment.CenterVertically) {
            NDLabel(text, Modifier.weight(1f), color = ND.colors.accent)
            NDButton("Fix", onFix, style = NDButtonStyle.Destructive)
        }
    }

    if (!notificationsOk) Warning("Notifications are off. Alarms can't show") {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else appSettings()
    }
    if (!fullScreenOk) Warning("Full-screen alarms are off") {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")),
        )
    }
    if (!exactOk) Warning("Exact alarms are off. Alarms may be late") {
        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
    }
}

/** Which alarm the editor is showing. Lives outside the screen so the editor can cover the whole app. */
object AlarmEditorState {
    var editing = androidx.compose.runtime.mutableStateOf<EditorRequest?>(null)
    fun open(alarm: Alarm?) { editing.value = EditorRequest(alarm) }
    fun close() { editing.value = null }
}

data class EditorRequest(val alarm: Alarm?)

internal fun kindLabel(kind: AlarmKind) = when (kind) {
    AlarmKind.SINGLE -> "Single"
    AlarmKind.CUSTOM -> "Custom"
    AlarmKind.LINEAR -> "Linear"
    AlarmKind.LOG -> "Log"
}
