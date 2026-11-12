package com.cloc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cloc.ui.theme.ND

enum class NDButtonStyle { Primary, Secondary, Ghost, Destructive }

/** Pill-shaped button. Primary = solid white, Secondary = outline, Ghost = text only, Destructive = red outline. */
@Composable
fun NDButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NDButtonStyle = NDButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val c = ND.colors
    val (background, textColor, outline) = when (style) {
        NDButtonStyle.Primary     -> Triple(c.textDisplay, c.black, null)
        NDButtonStyle.Secondary   -> Triple(Color.Transparent, c.textDisplay, c.borderVisible)
        NDButtonStyle.Ghost       -> Triple(Color.Transparent, c.textDisplay, null)
        NDButtonStyle.Destructive -> Triple(Color.Transparent, c.accent, c.accent)
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (outline != null) Modifier.border(1.dp, outline, CircleShape) else Modifier)
            .ndClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), color = textColor, style = ND.type.button)
    }
}
