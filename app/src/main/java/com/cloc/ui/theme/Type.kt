package com.cloc.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cloc.R

// Numbers
val Ndot57     = FontFamily(Font(R.font.ndot57))
val Ndot57Caps = FontFamily(Font(R.font.ndot57_caps))

// Big headings only
val NType82Headline = FontFamily(Font(R.font.ntype82_headline))

// Everything else. One family, three weights.
val LetteraMono = FontFamily(
    Font(R.font.lettera_mono_light,  FontWeight.Light),
    Font(R.font.lettera_mono,        FontWeight.Normal),
    Font(R.font.lettera_mono_medium, FontWeight.Medium),
)

// Never let Android fake a weight that has no real font file
private val base = TextStyle(fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None)

object NDType {
    // Ndot 57 Caps: numbers. Same digits as Ndot 57, but its colon is two single dots,
    // like the Nothing lock-screen clock. Keep at 24sp and up; dot-matrix gets mushy when small.
    val numberXl = base.copy(fontFamily = Ndot57Caps, fontSize = 72.sp, lineHeight = 1.0.em,  letterSpacing = (-0.03).em)
    val numberLg = base.copy(fontFamily = Ndot57Caps, fontSize = 48.sp, lineHeight = 1.05.em, letterSpacing = (-0.02).em)
    val numberMd = base.copy(fontFamily = Ndot57Caps, fontSize = 32.sp, lineHeight = 1.1.em,  letterSpacing = (-0.02).em)
    val numberSm = base.copy(fontFamily = Ndot57Caps, fontSize = 24.sp, lineHeight = 1.1.em)

    // NType 82 Headline: big headings only
    val heading = base.copy(fontFamily = NType82Headline, fontSize = 32.sp, lineHeight = 1.15.em, letterSpacing = (-0.01).em)

    // Lettera Mono: all subtext
    val body    = base.copy(fontFamily = LetteraMono, fontSize = 15.sp, lineHeight = 1.5.em)
    val button  = base.copy(fontFamily = LetteraMono, fontSize = 13.sp, lineHeight = 1.2.em, letterSpacing = 0.06.em, fontWeight = FontWeight.Medium)
    val caption = base.copy(fontFamily = LetteraMono, fontSize = 12.sp, lineHeight = 1.4.em, letterSpacing = 0.04.em)
    val label   = base.copy(fontFamily = LetteraMono, fontSize = 11.sp, lineHeight = 1.2.em, letterSpacing = 0.08.em)
}

// So any built-in Material component still uses these fonts
val NothingTypography = Typography(
    displayLarge = NDType.numberXl, displayMedium = NDType.numberLg, displaySmall = NDType.numberMd,
    headlineLarge = NDType.heading, headlineMedium = NDType.heading, headlineSmall = NDType.heading,
    titleLarge = NDType.heading, titleMedium = NDType.body, titleSmall = NDType.body,
    bodyLarge = NDType.body, bodyMedium = NDType.body, bodySmall = NDType.caption,
    labelLarge = NDType.button, labelMedium = NDType.caption, labelSmall = NDType.label,
)
