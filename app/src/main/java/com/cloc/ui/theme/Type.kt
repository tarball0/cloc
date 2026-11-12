package com.cloc.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cloc.R


val Ndot57          = FontFamily(Font(R.font.ndot57))
val Ndot57Caps      = FontFamily(Font(R.font.ndot57_caps))
val NType82         = FontFamily(Font(R.font.ntype82))
val NType82Headline = FontFamily(Font(R.font.ntype82_headline))
val NType82Mono     = FontFamily(Font(R.font.ntype82_mono))

private val base = TextStyle(fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None)

object NDType {
    val displayXl  = base.copy(fontFamily = Ndot57, fontSize = 72.sp, lineHeight = 1.0.em, letterSpacing = (-0.03).em)
    val displayLg  = base.copy(fontFamily = Ndot57,          fontSize = 48.sp, lineHeight = 1.05.em, letterSpacing = (-0.02).em)
    val displayMd  = base.copy(fontFamily = Ndot57,          fontSize = 36.sp, lineHeight = 1.1.em,  letterSpacing = (-0.02).em)
    val heading    = base.copy(fontFamily = NType82Headline, fontSize = 24.sp, lineHeight = 1.2.em,  letterSpacing = (-0.01).em)
    val subheading = base.copy(fontFamily = NType82,         fontSize = 18.sp, lineHeight = 1.3.em)
    val body       = base.copy(fontFamily = NType82,         fontSize = 16.sp, lineHeight = 1.5.em)
    val bodySm     = base.copy(fontFamily = NType82,         fontSize = 14.sp, lineHeight = 1.5.em,  letterSpacing = 0.01.em)
    val button     = base.copy(fontFamily = NType82Mono,     fontSize = 13.sp, lineHeight = 1.2.em,  letterSpacing = 0.06.em)
    val caption    = base.copy(fontFamily = NType82Mono,     fontSize = 12.sp, lineHeight = 1.4.em,  letterSpacing = 0.04.em)
    val label      = base.copy(fontFamily = NType82Mono,     fontSize = 11.sp, lineHeight = 1.2.em,  letterSpacing = 0.08.em)
    val data       = base.copy(fontFamily = NType82Mono,     fontSize = 16.sp, lineHeight = 1.3.em)
}

val NothingTypography = Typography(
    displayLarge = NDType.displayXl, displayMedium = NDType.displayLg, displaySmall = NDType.displayMd,
    headlineLarge = NDType.heading, headlineMedium = NDType.heading, headlineSmall = NDType.subheading,
    titleLarge = NDType.heading, titleMedium = NDType.subheading, titleSmall = NDType.bodySm,
    bodyLarge = NDType.body, bodyMedium = NDType.bodySm, bodySmall = NDType.caption,
    labelLarge = NDType.button, labelMedium = NDType.caption, labelSmall = NDType.label,
)
