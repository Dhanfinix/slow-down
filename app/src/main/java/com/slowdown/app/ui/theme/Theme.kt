package com.slowdown.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Light: values from docs/stitch DESIGN.md. Dark: derived from the same sage / clay hues (Stitch gave light only).
private val LightScheme = lightColorScheme(
    primary = Color(0xFF36533C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4D6B53),
    onPrimaryContainer = Color(0xFFC8EACC),
    inversePrimary = Color(0xFFAECFB2),
    secondary = Color(0xFF4F6353),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE5D1),
    onSecondaryContainer = Color(0xFF546757),
    tertiary = Color(0xFF793A23),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF975138),
    onTertiaryContainer = Color(0xFFFFDACE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF3FCF1),
    onBackground = Color(0xFF161D17),
    surface = Color(0xFFF3FCF1),
    onSurface = Color(0xFF161D17),
    surfaceVariant = Color(0xFFDCE5DA),
    onSurfaceVariant = Color(0xFF424842),
    surfaceTint = Color(0xFF47654E),
    inverseSurface = Color(0xFF2A322B),
    inverseOnSurface = Color(0xFFEBF3E8),
    outline = Color(0xFF727972),
    outlineVariant = Color(0xFFC2C8C0),
    surfaceDim = Color(0xFFD4DCD2),
    surfaceBright = Color(0xFFF3FCF1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF6EB),
    surfaceContainer = Color(0xFFE8F0E6),
    surfaceContainerHigh = Color(0xFFE2EBE0),
    surfaceContainerHighest = Color(0xFFDCE5DA),
    primaryFixed = Color(0xFFC9EBCD),
    primaryFixedDim = Color(0xFFAECFB2),
    onPrimaryFixed = Color(0xFF04210E),
    onPrimaryFixedVariant = Color(0xFF304D37),
    secondaryFixed = Color(0xFFD2E8D4),
    secondaryFixedDim = Color(0xFFB6CCB9),
    onSecondaryFixed = Color(0xFF0D1F13),
    onSecondaryFixedVariant = Color(0xFF384B3C),
    tertiaryFixed = Color(0xFFFFDBCF),
    tertiaryFixedDim = Color(0xFFFFB59C),
    onTertiaryFixed = Color(0xFF380C00),
    onTertiaryFixedVariant = Color(0xFF72351E),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFAECFB2),
    onPrimary = Color(0xFF1A3721),
    primaryContainer = Color(0xFF304D37),
    onPrimaryContainer = Color(0xFFC9EBCD),
    inversePrimary = Color(0xFF36533C),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3C),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = Color(0xFFFFB59C),
    onTertiary = Color(0xFF5A1F0A),
    tertiaryContainer = Color(0xFF72351E),
    onTertiaryContainer = Color(0xFFFFDBCF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDEE4DC),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDEE4DC),
    surfaceVariant = Color(0xFF424842),
    onSurfaceVariant = Color(0xFFC2C8C0),
    surfaceTint = Color(0xFFAECFB2),
    inverseSurface = Color(0xFFDEE4DC),
    inverseOnSurface = Color(0xFF2A322B),
    outline = Color(0xFF8C928B),
    outlineVariant = Color(0xFF424842),
    surfaceDim = Color(0xFF0F1511),
    surfaceBright = Color(0xFF353B36),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF262C27),
    surfaceContainerHighest = Color(0xFF303632),
    primaryFixed = Color(0xFFC9EBCD),
    primaryFixedDim = Color(0xFFAECFB2),
    onPrimaryFixed = Color(0xFF04210E),
    onPrimaryFixedVariant = Color(0xFF304D37),
    secondaryFixed = Color(0xFFD2E8D4),
    secondaryFixedDim = Color(0xFFB6CCB9),
    onSecondaryFixed = Color(0xFF0D1F13),
    onSecondaryFixedVariant = Color(0xFF384B3C),
    tertiaryFixed = Color(0xFFFFDBCF),
    tertiaryFixedDim = Color(0xFFFFB59C),
    onTertiaryFixed = Color(0xFF380C00),
    onTertiaryFixedVariant = Color(0xFF72351E),
)

private val SlowShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Brand palette always. Dynamic color is off on purpose so the sage and clay palette shows. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SlowDownTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = SlowTypography,
        shapes = SlowShapes,
        motionScheme = remember { MotionScheme.expressive() },
        content = content,
    )
}
