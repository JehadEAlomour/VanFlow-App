package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,

    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,

    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val warning30: Color,

    val surfaceHigh: Color,
    val surfaceLow: Color,

    val textPrimary: Color,
    val textSecondary: Color,

    val primary300: Color,
    val primary400: Color,

    val secondary50: Color,
    val secondary100: Color,
    val secondary400: Color,
    val secondary800: Color,
    val secondary950: Color,

    val gray150: Color,
    val gray250: Color,
    val gray400: Color,

    val starGold: Color,
)

val LightExtendedColors = ExtendedColors(
    success          = ColorTokens.Success100,
    onSuccess        = ColorTokens.White,
    successContainer = ColorTokens.Success10,

    info          = ColorTokens.Info100,
    onInfo        = ColorTokens.Gray200,
    infoContainer = ColorTokens.Info10,

    warning          = ColorTokens.Warning100,
    onWarning        = ColorTokens.Black,
    warningContainer = ColorTokens.Warning10,
    warning30        = ColorTokens.Warning30,

    surfaceHigh = ColorTokens.Primary800,
    surfaceLow  = ColorTokens.Primary300,

    textPrimary   = ColorTokens.TextPrimary,
    textSecondary = ColorTokens.TextSecondary,

    primary300 = ColorTokens.Primary300,
    primary400 = ColorTokens.Primary400,

    secondary50  = ColorTokens.Secondary50,
    secondary100 = ColorTokens.Secondary100,
    secondary400 = ColorTokens.Secondary400,
    secondary800 = ColorTokens.Secondary800,
    secondary950 = ColorTokens.Secondary950,

    gray150 = ColorTokens.Gray150,
    gray250 = ColorTokens.Gray250,
    gray400 = ColorTokens.Gray400,

    starGold = ColorTokens.StarGold,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
