package com.cloc.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cloc.ui.theme.ND
import com.cloc.ui.theme.ndMicro
import com.cloc.util.TimeFormat
import java.time.DayOfWeek
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A time of day: the number in Ndot, the AM/PM (if any) in small Lettera. */
@Composable
fun TimeText(
    minutesOfDay: Int,
    is24h: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = ND.colors.textDisplay,
) {
    val (num, suffix) = TimeFormat.clock(minutesOfDay, is24h)
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(num, style = style, color = color, maxLines = 1, softWrap = false)
        if (suffix != null) {
            Text(" $suffix", style = ND.type.caption, color = color, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

/** On/off switch in Nothing colours: white track when on, grey when off. */
@Composable
fun NDSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val c = ND.colors
    Switch(
        checked = checked,
        onCheckedChange = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onCheckedChange(it)
        },
        modifier = modifier,
        thumbContent = null,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.textDisplay,
            checkedThumbColor = c.black,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = c.borderVisible,
            uncheckedThumbColor = c.textDisabled,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Thin line between list rows. */
@Composable
fun NDDivider(modifier: Modifier = Modifier) =
    HorizontalDivider(modifier, thickness = 1.dp, color = ND.colors.border)

/** Row of options in a pill; the chosen one is inverted (white with black text). */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val c = ND.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .border(1.dp, c.borderVisible, CircleShape)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val bg by animateColorAsState(if (active) c.textDisplay else Color.Transparent, ndMicro(), label = "segBg")
            val fg by animateColorAsState(if (active) c.black else c.textDisplay, ndMicro(), label = "segFg")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .ndClickable {
                        if (!active) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelect(option)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option).uppercase(), style = ND.type.button, color = fg)
            }
        }
    }
}

/** Seven round toggles, Monday first. [days] is the bitmask from [com.cloc.data.Alarm]. */
@Composable
fun DayPicker(days: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val c = ND.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val bit = 1 shl (day.value - 1)
            val on = days and bit != 0
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (on) c.textDisplay else Color.Transparent)
                    .border(1.dp, if (on) c.textDisplay else c.borderVisible, CircleShape)
                    .ndClickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onChange(days xor bit)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(day.name.take(1), style = ND.type.caption, color = if (on) c.black else c.textDisplay)
            }
        }
    }
}

/** Short text for a days bitmask: ONCE, EVERY DAY, WEEKDAYS, TUE – THU, MON WED FRI … */
fun daysLabel(days: Int): String {
    if (days == 0) return "Once"
    if (days == 0b1111111) return "Every day"
    if (days == 0b0011111) return "Weekdays"
    if (days == 0b1100000) return "Weekends"
    val on = DayOfWeek.entries.filter { days and (1 shl (it.value - 1)) != 0 }
    val contiguous = on.size >= 3 && on.zipWithNext().all { (a, b) -> b.value == a.value + 1 }
    fun short(d: DayOfWeek) = d.name.take(3)
    return if (contiguous) "${short(on.first())} – ${short(on.last())}" else on.joinToString(" ") { short(it) }
}

/**
 * − value + . Tap to change by one; hold to keep changing.
 */
@Composable
fun NDStepper(
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    unit: String = "",
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    @Composable
    fun StepButton(type: NDIconType, delta: Int) {
        val enabled = (current + delta) in range
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, ND.colors.borderVisible, CircleShape)
                .pointerInput(delta) {
                    detectTapGestures(onPress = {
                        fun step(): Boolean {
                            val next = current + delta
                            if (next !in range) return false
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            change(next)
                            return true
                        }
                        step()
                        val job = scope.launch {
                            delay(400)
                            while (step()) delay(70)
                        }
                        tryAwaitRelease()
                        job.cancel()
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            NDIcon(type, size = 20.dp, color = if (enabled) ND.colors.textDisplay else ND.colors.textDisabled)
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepButton(NDIconType.Minus, -1)
        Row(
            Modifier.widthIn(min = 96.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("$value", style = ND.type.numberMd, color = ND.colors.textDisplay, textAlign = TextAlign.Center)
            if (unit.isNotEmpty()) {
                Text(" $unit", style = ND.type.caption, color = ND.colors.textDisplay, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        StepButton(NDIconType.Plus, +1)
    }
}

/** Small outline pill used for tags (CUSTOM, LOG …) and quick-add bubbles. */
@Composable
fun NDTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ND.colors.textDisplay,
    borderColor: Color = ND.colors.borderVisible,
    onClick: (() -> Unit)? = null,
    filled: Boolean = false,
) {
    val view = LocalView.current
    Box(
        modifier
            .clip(CircleShape)
            .background(if (filled) ND.colors.textDisplay else Color.Transparent)
            .border(1.dp, if (filled) ND.colors.textDisplay else borderColor, CircleShape)
            .then(
                if (onClick != null) Modifier.ndClickable {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onClick()
                } else Modifier,
            )
            .padding(horizontal = if (onClick != null) 16.dp else 10.dp, vertical = if (onClick != null) 12.dp else 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = ND.type.caption, color = if (filled) ND.colors.black else color)
    }
}
