package com.cloc.ui.alarm

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cloc.alarm.BatchMath
import com.cloc.data.Alarm
import com.cloc.data.AlarmKind
import com.cloc.data.AlarmStore
import com.cloc.ui.components.DayPicker
import com.cloc.ui.components.NDButton
import com.cloc.ui.components.NDButtonStyle
import com.cloc.ui.components.NDDivider
import com.cloc.ui.components.NDIconButton
import com.cloc.ui.components.NDIconType
import com.cloc.ui.components.NDLabel
import com.cloc.ui.components.NDStepper
import com.cloc.ui.components.NDTag
import com.cloc.ui.components.SegmentedControl
import com.cloc.ui.components.TimeWheel
import com.cloc.ui.components.daysLabel
import com.cloc.ui.theme.ND
import com.cloc.util.TimeFormat
import java.time.LocalTime

/**
 * Full-screen editor for a new or existing alarm.
 * SINGLE | BATCH at the top; a batch is CUSTOM, LINEAR or LOG.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmEditor(initial: Alarm?, onClose: () -> Unit) {
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
    val defaultStart = initial?.start ?: ((nowMinute / 5 + 1) * 5 % 1440)

    var kind by remember { mutableStateOf(initial?.kind ?: AlarmKind.SINGLE) }
    var batchKind by remember { mutableStateOf(initial?.kind?.takeIf { it != AlarmKind.SINGLE } ?: AlarmKind.CUSTOM) }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var days by remember { mutableIntStateOf(initial?.days ?: 0) }
    var start by remember { mutableIntStateOf(defaultStart) }
    var interval by remember { mutableIntStateOf(initial?.interval ?: 30) }
    var count by remember { mutableIntStateOf(if (initial?.kind == AlarmKind.LINEAR) initial.count else 4) }
    // LOG starts at the largest count that still keeps every gap ≥ 1 minute, until changed.
    var logCount by remember { mutableStateOf(if (initial?.kind == AlarmKind.LOG) initial.count else null) }
    var end by remember { mutableIntStateOf((initial?.end ?: (defaultStart + 60)) % 1440) }
    // Custom chain: minutes after the first alarm, e.g. [30, 45, 55].
    var offsets by remember {
        mutableStateOf<List<Int>>(
            if (initial?.kind == AlarmKind.CUSTOM) initial.times.drop(1).map { it - initial.times.first() } else emptyList(),
        )
    }
    var customOpen by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableIntStateOf(15) }

    val effectiveKind = if (kind == AlarmKind.SINGLE) AlarmKind.SINGLE else batchKind
    val logSpan = BatchMath.span(start, end)
    val maxLog = BatchMath.maxLogCount(logSpan)
    val maxLinear = BatchMath.maxLinearCount(start, interval)

    val times: List<Int> = when (effectiveKind) {
        AlarmKind.SINGLE -> listOf(start)
        AlarmKind.CUSTOM -> BatchMath.custom(start, offsets)
        AlarmKind.LINEAR -> BatchMath.linear(start, interval, count.coerceIn(2, maxLinear.coerceAtLeast(2)))
        AlarmKind.LOG -> BatchMath.logarithmic(start, end, (logCount ?: maxLog).coerceIn(2, maxLog))
    }

    fun save() {
        AlarmStore.save(
            Alarm(
                id = initial?.id ?: AlarmStore.nextId(),
                label = label.trim(),
                kind = effectiveKind,
                days = days,
                times = times,
                enabled = true,
                skipDate = null,
                oneOffDate = null,
                start = start,
                interval = interval,
                count = times.size,
                end = end,
            ),
        )
        onClose()
    }

    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(ND.colors.black).systemBarsPadding().imePadding()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(start = ND.space.md, end = ND.space.xs, top = ND.space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NDLabel(if (initial == null) "New alarm" else "Edit alarm", Modifier.weight(1f), color = ND.colors.textDisplay)
            NDIconButton(NDIconType.Close, onClose, outlined = false)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ND.space.md),
        ) {
            Spacer(Modifier.height(ND.space.md))
            SegmentedControl(
                options = listOf(AlarmKind.SINGLE, AlarmKind.CUSTOM),
                selected = if (kind == AlarmKind.SINGLE) AlarmKind.SINGLE else AlarmKind.CUSTOM,
                onSelect = { kind = if (it == AlarmKind.SINGLE) AlarmKind.SINGLE else batchKind },
                label = { if (it == AlarmKind.SINGLE) "Single" else "Batch" },
            )
            if (kind != AlarmKind.SINGLE) {
                Spacer(Modifier.height(ND.space.sm))
                SegmentedControl(
                    options = listOf(AlarmKind.CUSTOM, AlarmKind.LINEAR, AlarmKind.LOG),
                    selected = batchKind,
                    onSelect = { batchKind = it; kind = it },
                    label = ::kindLabel,
                )
            }

            Section("Label")
            BasicTextField(
                value = label,
                onValueChange = { if (it.length <= 24) label = it },
                singleLine = true,
                textStyle = ND.type.body.copy(color = ND.colors.textDisplay),
                cursorBrush = SolidColor(ND.colors.textDisplay),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    val line = ND.colors.borderVisible
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                            }
                            .padding(vertical = ND.space.sm),
                    ) {
                        if (label.isEmpty()) NDLabel("Optional")
                        inner()
                    }
                },
            )

            Section("Repeat · ${daysLabel(days)}")
            DayPicker(days, { days = it })

            when (effectiveKind) {
                AlarmKind.SINGLE -> {
                    Section("Time")
                    TimeWheel(start, { start = it }, is24h)
                }

                AlarmKind.CUSTOM -> {
                    Section("First alarm")
                    TimeWheel(start, { start = it }, is24h)
                    Section("Add after the last alarm (minutes)")
                    fun add(minutes: Int) {
                        val last = offsets.lastOrNull() ?: 0
                        val next = last + minutes
                        if (start + next <= BatchMath.MAX_MINUTE && offsets.size + 1 < BatchMath.MAX_ALARMS) {
                            offsets = offsets + next
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ND.space.sm),
                        verticalArrangement = Arrangement.spacedBy(ND.space.sm),
                    ) {
                        listOf(2, 5, 10, 20, 30, 60).forEach { m -> NDTag("+$m", onClick = { add(m) }) }
                        NDTag("Custom", onClick = { customOpen = !customOpen }, filled = customOpen)
                    }
                    if (customOpen) {
                        Spacer(Modifier.height(ND.space.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NDStepper(customMinutes, { customMinutes = it }, 1..240, unit = "min")
                            Spacer(Modifier.weight(1f))
                            NDButton("Add +$customMinutes", { add(customMinutes) })
                        }
                    }
                }

                AlarmKind.LINEAR -> {
                    Section("Start")
                    TimeWheel(start, { start = it }, is24h)
                    Section("Every")
                    NDStepper(interval, { interval = it }, 1..240, unit = "min")
                    Section("Number of alarms")
                    NDStepper(count.coerceIn(2, maxLinear.coerceAtLeast(2)), { count = it }, 2..maxLinear.coerceAtLeast(2))
                }

                AlarmKind.LOG -> {
                    Section("Start")
                    TimeWheel(start, { start = it }, is24h)
                    Section("Must be up by")
                    TimeWheel(end, { end = it }, is24h)
                    Section("Number of alarms · max $maxLog")
                    NDStepper((logCount ?: maxLog).coerceIn(2, maxLog), { logCount = it }, 2..maxLog)
                    Spacer(Modifier.height(ND.space.sm))
                    NDLabel("Each gap is half the one before, rounded to whole minutes")
                }
            }

            if (effectiveKind != AlarmKind.SINGLE) {
                Section("Preview · ${times.size} alarms")
                times.forEachIndexed { i, t ->
                    if (i > 0) NDDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            TimeFormat.clockText(t, is24h),
                            style = ND.type.body,
                            color = ND.colors.textDisplay,
                            modifier = Modifier.weight(1f),
                        )
                        if (t >= 1440) NDLabel("+1D  ")
                        NDLabel(if (i == 0) "First" else TimeFormat.gap(t - times[i - 1]), Modifier.width(56.dp))
                        if (effectiveKind == AlarmKind.CUSTOM && i > 0) {
                            NDIconButton(
                                NDIconType.Close,
                                {
                                    val offset = t - start
                                    offsets = offsets.filterNot { it == offset }
                                },
                                outlined = false,
                                size = 36.dp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(ND.space.xl))
        }

        // Bottom buttons
        Column(Modifier.padding(ND.space.md)) {
            NDButton(
                "Save",
                ::save,
                Modifier.fillMaxWidth(),
                enabled = effectiveKind == AlarmKind.SINGLE || times.size >= 2,
            )
            if (initial != null) {
                Spacer(Modifier.height(ND.space.sm))
                NDButton(
                    "Delete",
                    { AlarmStore.delete(initial.id); onClose() },
                    Modifier.fillMaxWidth(),
                    NDButtonStyle.Destructive,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(ND.space.xl))
    NDLabel(title)
    Spacer(Modifier.height(ND.space.sm))
}
