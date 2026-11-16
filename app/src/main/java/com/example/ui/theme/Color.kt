package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Finnish Flag & Winter Aurora Palette
val FinnishBlue = Color(0xFF002F6C)       // Pantone 294C
val FinnishBlueDeep = Color(0xFF001E47)
val FinnishBlueBright = Color(0xFF0B4DA2)
val PureSnowWhite = Color(0xFFFFFFFF)
val FrostCanvasLight = Color(0xFFF3F8FF)
val IceSurfaceLight = Color(0xFFE5F0FC)
val IceBorderLight = Color(0xFF002F6C)
val CelShadowLight = Color(0xFFA9C4E4)

val MidnightAbyss = Color(0xFF040D1A)
val DeepCelNavy = Color(0xFF081B36)
val ElevatedCelNavy = Color(0xFF0F294D)
val IceCyanGlow = Color(0xFF00E5FF)
val ElectricIce = Color(0xFF29B6F6)
val AuroraTeal = Color(0xFF00E6B8)
val AuroraIndigo = Color(0xFF4F8BFF)
val SilverFrost = Color(0xFFC5D6EA)

val CelGold = Color(0xFFFFD166)
val CelGoldDeep = Color(0xFFE09F1F)
val StatusOnTime = Color(0xFF00C48C)
val StatusLate = Color(0xFFFFB703)
val StatusMissed = Color(0xFFFF4757)
val StatusQada = Color(0xFF4EA8DE)

@Immutable
data class CelColors(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val cardSurface: Color,
    val cardSurfaceAlt: Color,
    val cardBorder: Color,
    val celShadow: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val auroraGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val goldAccent: Color,
    val onTimeColor: Color,
    val lateColor: Color,
    val missedColor: Color,
    val qadaColor: Color,
    val snowflakeColor: Color,
)

val LightCelColors = CelColors(
    isDark = false,
    backgroundTop = Color(0xFFF6FAFF),
    backgroundBottom = Color(0xFFE4EFFC),
    cardSurface = PureSnowWhite,
    cardSurfaceAlt = Color(0xFFEAF3FE),
    cardBorder = FinnishBlue,
    celShadow = Color(0xFF9FBBE0),
    primaryAccent = FinnishBlue,
    secondaryAccent = Color(0xFF0077CC),
    auroraGlow = Color(0xFF00A8CC),
    textPrimary = Color(0xFF061A38),
    textSecondary = Color(0xFF3E5A80),
    goldAccent = Color(0xFFD98E04),
    onTimeColor = Color(0xFF00875A),
    lateColor = Color(0xFFD97706),
    missedColor = Color(0xFFD62839),
    qadaColor = Color(0xFF0077B6),
    snowflakeColor = Color(0xFF5B9BD5),
)

val DarkCelColors = CelColors(
    isDark = true,
    backgroundTop = MidnightAbyss,
    backgroundBottom = Color(0xFF071A34),
    cardSurface = DeepCelNavy,
    cardSurfaceAlt = ElevatedCelNavy,
    cardBorder = ElectricIce,
    celShadow = Color(0xFF01060E),
    primaryAccent = IceCyanGlow,
    secondaryAccent = AuroraTeal,
    auroraGlow = AuroraIndigo,
    textPrimary = PureSnowWhite,
    textSecondary = SilverFrost,
    goldAccent = CelGold,
    onTimeColor = AuroraTeal,
    lateColor = CelGold,
    missedColor = StatusMissed,
    qadaColor = ElectricIce,
    snowflakeColor = Color(0xFFB3E5FC),
)

val LocalCelColors = staticCompositionLocalOf { DarkCelColors }
