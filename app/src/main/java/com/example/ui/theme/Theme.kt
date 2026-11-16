package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = IceCyanGlow,
    onPrimary = MidnightAbyss,
    primaryContainer = FinnishBlue,
    onPrimaryContainer = PureSnowWhite,
    secondary = AuroraTeal,
    onSecondary = MidnightAbyss,
    secondaryContainer = ElevatedCelNavy,
    onSecondaryContainer = SilverFrost,
    tertiary = CelGold,
    onTertiary = MidnightAbyss,
    background = MidnightAbyss,
    onBackground = PureSnowWhite,
    surface = DeepCelNavy,
    onSurface = PureSnowWhite,
    surfaceVariant = ElevatedCelNavy,
    onSurfaceVariant = SilverFrost,
    outline = ElectricIce,
    error = StatusMissed,
    onError = PureSnowWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = FinnishBlue,
    onPrimary = PureSnowWhite,
    primaryContainer = Color(0xFFD6E6FB),
    onPrimaryContainer = FinnishBlueDeep,
    secondary = Color(0xFF0077CC),
    onSecondary = PureSnowWhite,
    secondaryContainer = IceSurfaceLight,
    onSecondaryContainer = FinnishBlueDeep,
    tertiary = Color(0xFFD98E04),
    onTertiary = PureSnowWhite,
    background = FrostCanvasLight,
    onBackground = Color(0xFF061A38),
    surface = PureSnowWhite,
    onSurface = Color(0xFF061A38),
    surfaceVariant = IceSurfaceLight,
    onSurfaceVariant = Color(0xFF3E5A80),
    outline = FinnishBlue,
    error = Color(0xFFD62839),
    onError = PureSnowWhite,
)

private val CelShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun TariqAlJannahTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val celColors = if (darkTheme) DarkCelColors else LightCelColors

    CompositionLocalProvider(
        LocalCelColors provides celColors,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CelShapes,
            content = content,
        )
    }
}
