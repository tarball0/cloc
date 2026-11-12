package com.cloc.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cloc.ui.theme.ND

/** Small all-caps Lettera Mono text, for labels and subtext. */
@Composable
fun NDLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ND.colors.textSecondary,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = ND.type.label,
    )
}
