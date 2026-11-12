package com.cloc.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cloc.ui.theme.ND
import com.cloc.ui.theme.ndMicro

/** The four sections of the app, in the order they appear in the bottom bar. */
enum class Tab(val label: String) {
    Clock("Clock"),
    Alarm("Alarm"),
    Timer("Timer"),
    Stopwatch("Stopwatch"),
}

/** Text-only bottom bar. The active tab is white with a small dot under it. */
@Composable
fun BottomNav(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ND.colors.black)
            .navigationBarsPadding()
            .height(64.dp),
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            val textColor by animateColorAsState(
                targetValue = if (active) ND.colors.textDisplay else ND.colors.textDisabled,
                animationSpec = ndMicro(),
                label = "tabText",
            )
            val dotColor by animateColorAsState(
                targetValue = if (active) ND.colors.accent else Color.Transparent,
                animationSpec = ndMicro(),
                label = "tabDot",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ndClickable(role = Role.Tab) {
                        if (!active) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelect(tab)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                NDLabel(tab.label, color = textColor)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(4.dp).background(dotColor, CircleShape))
            }
        }
    }
}
