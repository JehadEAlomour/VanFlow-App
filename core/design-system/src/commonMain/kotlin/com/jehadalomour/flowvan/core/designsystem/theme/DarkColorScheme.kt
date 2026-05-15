package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.material3.darkColorScheme

val DarkColorScheme = darkColorScheme(
    // ===== Brand =====
    primary            = ColorTokens.FvAccentBlue,
    onPrimary          = ColorTokens.White,
    primaryContainer   = ColorTokens.FvSurfaceTop,
    onPrimaryContainer = ColorTokens.FvTextHigh,

    secondary            = ColorTokens.FvAccentTeal,
    onSecondary          = ColorTokens.FvBgDeepest,
    secondaryContainer   = ColorTokens.FvSurfaceHigh,
    onSecondaryContainer = ColorTokens.FvTextHigh,

    // ===== Background / Surface =====
    background       = ColorTokens.FvBgDeepest,
    onBackground     = ColorTokens.FvTextHigh,
    surface          = ColorTokens.FvSurface,
    onSurface        = ColorTokens.FvTextHigh,
    surfaceVariant   = ColorTokens.FvSurfaceHigh,
    onSurfaceVariant = ColorTokens.FvTextMid,
    outlineVariant   = ColorTokens.FvBorder,
    outline          = ColorTokens.FvTextLow,

    // ===== Error =====
    error   = ColorTokens.FvAccentRed,
    onError = ColorTokens.White,
)

val DarkExtendedColors = ExtendedColors(
    success          = ColorTokens.FvAccentGreen,
    onSuccess        = ColorTokens.FvBgDeepest,
    successContainer = ColorTokens.FvSurfaceHigh,

    info          = ColorTokens.FvAccentBlue,
    onInfo        = ColorTokens.White,
    infoContainer = ColorTokens.FvSurfaceHigh,

    warning          = ColorTokens.FvAccentAmber,
    onWarning        = ColorTokens.FvBgDeepest,
    warningContainer = ColorTokens.FvSurfaceHigh,
    warning30        = ColorTokens.FvAccentAmber,

    surfaceHigh = ColorTokens.FvSurfaceTop,
    surfaceLow  = ColorTokens.FvBg,

    textPrimary   = ColorTokens.FvTextHigh,
    textSecondary = ColorTokens.FvTextMid,

    primary300 = ColorTokens.FvAccentBlue,
    primary400 = ColorTokens.FvAccentBlue,

    secondary50  = ColorTokens.FvSurface,
    secondary100 = ColorTokens.FvSurfaceHigh,
    secondary400 = ColorTokens.FvAccentTeal,
    secondary800 = ColorTokens.FvSurfaceTop,
    secondary950 = ColorTokens.FvBgDeepest,

    gray150 = ColorTokens.FvSurfaceHigh,
    gray250 = ColorTokens.FvTextLow,
    gray400 = ColorTokens.FvTextMid,

    starGold = ColorTokens.StarGold,
)