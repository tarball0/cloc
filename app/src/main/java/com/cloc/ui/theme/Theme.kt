package com.cloc.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

object NDSpace {
    val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp
    val xl = 32.dp; val xxl = 48.dp; val xxxl = 64.dp; val xxxxl = 96.dp
}

val NothingShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

val NDEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
fun <T> ndMicro() = tween<T>(200, easing = NDEasing)
fun <T> ndTransition() = tween<T>(350, easing = NDEasing)

val LocalNothingColors = staticCompositionLocalOf { DarkNothing }

object ND {
    val colors: NothingColors @Composable @ReadOnlyComposable get() = LocalNothingColors.current
    val type = NDType
    val space = NDSpace
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkNothing.toMaterial(),
        typography = NothingTypography,
        shapes = NothingShapes,
    ) {
        CompositionLocalProvider(
            LocalNothingColors provides DarkNothing,
            LocalRippleConfiguration provides null,
            LocalContentColor provides DarkNothing.textPrimary,
            content = content,
        )
    }
}