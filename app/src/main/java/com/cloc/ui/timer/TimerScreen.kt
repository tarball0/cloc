package com.cloc.ui.timer

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloc.data.TimerItem
import com.cloc.data.TimerStore
import com.cloc.ui.components.NDButton
import com.cloc.ui.components.NDDivider
import com.cloc.ui.components.NDIconButton
import com.cloc.ui.components.NDIconType
import com.cloc.ui.components.NDLabel
import com.cloc.ui.components.SegmentedBar
import com.cloc.ui.components.ndClickable
import com.cloc.ui.theme.ND
import com.cloc.util.TimeFormat
import kotlinx.coroutines.delay

/** A clock value that updates every [periodMs] while [active]. */
@Composable
fun rememberNow(active: Boolean, periodMs: Long = 100): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        now = System.currentTimeMillis()
        while (active) {
            delay(periodMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** Timer tab: a list of timers, each with its time, a progress bar and play/pause + reset. */
@Composable
fun TimerScreen() {
    val timers by TimerStore.timers.collectAsState()
    val now = rememberNow(timers.any { it.running })
    var showAdd by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = ND.space.md)) {
        Row(
            Modifier.fillMaxWidth().padding(top = ND.space.xl, bottom = ND.space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Timers", style = ND.type.heading, color = ND.colors.textDisplay, modifier = Modifier.weight(1f))
            NDButton("+ Timer", { showAdd = true })
        }

        if (timers.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                NDLabel("No timers yet")
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(timers, key = { _, t -> t.id }) { idx, timer ->
                    if (idx > 0) NDDivider()
                    TimerRow(timer, now, onOpen = { openId = timer.id })
                }
            }
        }
    }

    if (showAdd) AddTimerDialog(onDismiss = { showAdd = false })

    val open = timers.firstOrNull { it.id == openId }
    if (open != null) SandTimerDialog(open, onDismiss = { openId = null })
}

@Composable
private fun TimerRow(timer: TimerItem, now: Long, onOpen: () -> Unit) {
    val remaining = timer.remainingAt(now)
    val done = timer.finished || (timer.running && remaining <= 0)
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (timer.started) Modifier.ndClickable(onClick = onOpen) else Modifier)
            .padding(vertical = ND.space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                NDLabel(
                    if (done) "Done" else TimeFormat.durationLabel(timer.durationMs),
                    color = if (done) ND.colors.accent else ND.colors.textSecondary,
                )
                Spacer(Modifier.height(ND.space.xs))
                Text(
                    TimeFormat.countdown(remaining),
                    style = if (timer.running) ND.type.numberLg else ND.type.numberMd,
                    color = ND.colors.textDisplay,
                    maxLines = 1,
                )
            }
            NDIconButton(NDIconType.Reset, { TimerStore.reset(timer.id) }, enabled = timer.started)
            Spacer(Modifier.width(ND.space.sm))
            NDIconButton(
                if (timer.running) NDIconType.Pause else NDIconType.Play,
                { TimerStore.toggle(timer.id) },
                filled = true,
            )
            Spacer(Modifier.width(ND.space.xs))
            NDIconButton(NDIconType.Close, { TimerStore.delete(timer.id) }, outlined = false, size = 40.dp)
        }
        Spacer(Modifier.height(ND.space.md))
        SegmentedBar(
            value = (timer.durationMs - remaining).toFloat(),
            max = timer.durationMs.toFloat().coerceAtLeast(1f),
            height = 8.dp,
            segments = 24,
        )
    }
}

@Composable
private fun AddTimerDialog(onDismiss: () -> Unit) {
    var h by remember { mutableStateOf(0) }
    var m by remember { mutableStateOf(5) }
    var s by remember { mutableStateOf(0) }
    var presetKey by remember { mutableStateOf(0) }
    val totalMs = ((h * 3600L) + m * 60L + s) * 1000L

    NDDialogCard(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NDLabel("New timer", Modifier.weight(1f), color = ND.colors.textDisplay)
            NDIconButton(NDIconType.Close, onDismiss, outlined = false, size = 40.dp)
        }
        Spacer(Modifier.height(ND.space.md))
        // Changing presetKey rebuilds the wheels so they jump to the preset.
        androidx.compose.runtime.key(presetKey) {
            com.cloc.ui.components.DurationWheel(h, m, s, onChange = { nh, nm, ns -> h = nh; m = nm; s = ns })
        }
        Spacer(Modifier.height(ND.space.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ND.space.xs)) {
            listOf(1, 3, 5, 10, 15, 30).forEach { preset ->
                com.cloc.ui.components.NDTag(
                    "$preset",
                    Modifier.weight(1f),
                    onClick = { h = 0; m = preset; s = 0; presetKey++ },
                )
            }
        }
        Spacer(Modifier.height(ND.space.xs))
        NDLabel("Presets in minutes")
        Spacer(Modifier.height(ND.space.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(ND.space.sm)) {
            NDButton(
                "Add", { TimerStore.add(totalMs, start = false); onDismiss() },
                Modifier.weight(1f), com.cloc.ui.components.NDButtonStyle.Secondary, enabled = totalMs > 0,
            )
            NDButton(
                "Start", { TimerStore.add(totalMs, start = true); onDismiss() },
                Modifier.weight(1f), enabled = totalMs > 0,
            )
        }
    }
}
