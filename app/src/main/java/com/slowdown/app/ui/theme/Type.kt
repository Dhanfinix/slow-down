package com.slowdown.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowdown.app.R

// Bundled variable fonts (res/font), so everything works offline. minSdk 26 supports variation settings.
@OptIn(ExperimentalTextApi::class)
private fun family(res: Int, style: FontStyle = FontStyle.Normal, vararg weights: Int) = FontFamily(
    weights.map { w ->
        Font(res, FontWeight(w), style, variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

val Newsreader = family(R.font.newsreader, FontStyle.Normal, 400, 500, 600)
val NewsreaderItalic = family(R.font.newsreader_italic, FontStyle.Italic, 400, 500)
val PlusJakarta = family(R.font.plus_jakarta_sans, FontStyle.Normal, 400, 500, 600, 700)
val JetBrainsMono = family(R.font.jetbrains_mono, FontStyle.Normal, 400, 500, 600)

private fun t(f: FontFamily, size: Int, line: Int, w: Int, ls: TextUnit = 0.sp) =
    TextStyle(fontFamily = f, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight(w), letterSpacing = ls)

/** DESIGN.md scale mapped onto Material roles. */
val SlowTypography = Typography(
    displayLarge = t(Newsreader, 44, 52, 400, (-0.02).em),
    displayMedium = t(Newsreader, 36, 44, 400, (-0.02).em),
    displaySmall = t(Newsreader, 32, 40, 400, (-0.02).em),
    headlineLarge = t(Newsreader, 30, 38, 500),
    headlineMedium = t(Newsreader, 24, 32, 500),
    headlineSmall = t(PlusJakarta, 18, 26, 700),
    titleLarge = t(PlusJakarta, 18, 26, 700),
    titleMedium = t(PlusJakarta, 16, 24, 600),
    titleSmall = t(PlusJakarta, 14, 20, 600),
    bodyLarge = t(PlusJakarta, 16, 24, 400),
    bodyMedium = t(PlusJakarta, 14, 20, 400),
    bodySmall = t(PlusJakarta, 12, 18, 400),
    labelLarge = t(PlusJakarta, 14, 20, 600),
    labelMedium = t(PlusJakarta, 12, 16, 600),
    labelSmall = t(PlusJakarta, 11, 14, 500),
)

/** Telemetry styles (JetBrains Mono) and the italic wordmark. */
object SlowText {
    val metricDisplay = t(JetBrainsMono, 28, 34, 600, (-0.03).em)
    val metricHero = t(JetBrainsMono, 40, 46, 600, (-0.03).em)
    val metricMid = t(JetBrainsMono, 18, 24, 600, (-0.02).em)
    val metricLabel = t(JetBrainsMono, 12, 16, 500, 0.04.em)
    val wordmark = t(NewsreaderItalic, 24, 32, 400)
}
