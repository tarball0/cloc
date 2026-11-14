package com.cloc.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * Text whose digits all take the same width, so a fast-changing number (a stopwatch)
 * doesn't jiggle sideways. Ndot has no tabular figures, so each digit gets its own slot
 * as wide as the widest digit; other characters (":" ".") keep their natural width.
 */
@Composable
fun TabularText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitWidth = remember(style, density) {
        with(density) { ('0'..'9').maxOf { measurer.measure(it.toString(), style).size.width }.toDp() }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEach { ch ->
            if (ch.isDigit()) {
                Box(Modifier.width(digitWidth), contentAlignment = Alignment.Center) {
                    Text(ch.toString(), style = style, color = color, maxLines = 1, softWrap = false)
                }
            } else {
                Text(ch.toString(), style = style, color = color, maxLines = 1, softWrap = false)
            }
        }
    }
}
