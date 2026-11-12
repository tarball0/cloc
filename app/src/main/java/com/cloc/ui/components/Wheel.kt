package com.cloc.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cloc.ui.theme.ND
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

private val ItemHeight = 52.dp

/**
 * A vertical scroll wheel showing three values; the middle one is selected.
 * [cyclic] wheels wrap around (59 → 00), like a real clock.
 */
@Composable
fun NumberWheel(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 76.dp,
    cyclic: Boolean = true,
    style: TextStyle = ND.type.numberMd,
) {
    val size = items.size
    val repeats = if (cyclic) 400 else 1
    val startIndex = if (cyclic) size * (repeats / 2) + selectedIndex else selectedIndex
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val itemPx = with(LocalDensity.current) { ItemHeight.toPx() }
    val view = LocalView.current
    val select by rememberUpdatedState(onSelect)

    // Which row sits in the middle right now.
    val center by remember {
        derivedStateOf {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { center }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                select(it % size)
            }
    }

    Box(modifier.width(width).height(ItemHeight * 3), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = ItemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(size * repeats) { i ->
                Box(Modifier.height(ItemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        items[i % size],
                        style = style,
                        color = if (i == center) ND.colors.textDisplay else ND.colors.textDisplay.copy(alpha = 0.22f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Hour and minute wheels. Uses AM/PM when the phone is set to 12-hour time.
 * [minutes] is the time of day in minutes (0..1439).
 */
@Composable
fun TimeWheel(minutes: Int, onChange: (Int) -> Unit, is24h: Boolean, modifier: Modifier = Modifier) {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    val current by rememberUpdatedState(minutes)
    val change by rememberUpdatedState(onChange)

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (is24h) {
            NumberWheel(
                items = List(24) { "%02d".format(it) },
                selectedIndex = h,
                onSelect = { nh -> change(nh * 60 + current % 60) },
            )
        } else {
            NumberWheel(
                items = List(12) { "%d".format(if (it == 0) 12 else it) },
                selectedIndex = h % 12,
                onSelect = { i -> val pm = (current / 60) >= 12; change(((i % 12) + if (pm) 12 else 0) * 60 + current % 60) },
            )
        }
        Text(":", style = ND.type.numberMd, color = ND.colors.textDisplay, modifier = Modifier.padding(horizontal = 4.dp))
        NumberWheel(
            items = List(60) { "%02d".format(it) },
            selectedIndex = m,
            onSelect = { nm -> change((current / 60) * 60 + nm) },
        )
        if (!is24h) {
            NumberWheel(
                items = listOf("AM", "PM"),
                selectedIndex = if (h >= 12) 1 else 0,
                onSelect = { i -> val h12 = (current / 60) % 12; change((h12 + if (i == 1) 12 else 0) * 60 + current % 60) },
                cyclic = false,
                width = 64.dp,
                style = ND.type.body,
            )
        }
    }
}

/** Hours / minutes / seconds wheels for timers, with tiny unit labels under them. */
@Composable
fun DurationWheel(h: Int, m: Int, s: Int, onChange: (Int, Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val ch by rememberUpdatedState(h)
    val cm by rememberUpdatedState(m)
    val cs by rememberUpdatedState(s)
    val change by rememberUpdatedState(onChange)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        listOf("H" to 24, "M" to 60, "S" to 60).forEachIndexed { idx, (unit, max) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumberWheel(
                    items = List(max) { "%02d".format(it) },
                    selectedIndex = when (idx) { 0 -> h; 1 -> m; else -> s },
                    onSelect = { v ->
                        when (idx) {
                            0 -> change(v, cm, cs)
                            1 -> change(ch, v, cs)
                            else -> change(ch, cm, v)
                        }
                    },
                )
                NDLabel(unit)
            }
            if (idx < 2) {
                Text(":", style = ND.type.numberMd, color = ND.colors.textDisplay, modifier = Modifier.padding(top = ItemHeight + 8.dp))
            }
        }
    }
}
