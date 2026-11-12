package com.cloc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.cloc.ui.theme.ndMicro

/**
 * Makes anything tappable, Nothing-style: no ripple, the element just dims while pressed.
 */
@Composable
fun Modifier.ndClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.4f
            pressed -> 0.6f
            else -> 1f
        },
        animationSpec = ndMicro(),
        label = "press",
    )
    return this
        .graphicsLayer { this.alpha = alpha }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}
