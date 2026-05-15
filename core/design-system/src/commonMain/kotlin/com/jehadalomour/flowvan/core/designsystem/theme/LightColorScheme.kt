package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.material3.lightColorScheme

val LightColorScheme = lightColorScheme(

    // ===== Brand =====
    primary            = ColorTokens.Primary500,
    onPrimary          = ColorTokens.White,
    primaryContainer   = ColorTokens.Primary100,
    onPrimaryContainer = ColorTokens.Primary950,

    secondary            = ColorTokens.Secondary500,
    onSecondary          = ColorTokens.White,
    secondaryContainer   = ColorTokens.Secondary100,
    onSecondaryContainer = ColorTokens.Secondary900,

    // ===== Background / Surface =====
    background       = ColorTokens.Background,
    onBackground     = ColorTokens.Black,
    surface          = ColorTokens.White,
    onSurface        = ColorTokens.Black,
    surfaceVariant   = ColorTokens.Primary50,
    onSurfaceVariant = ColorTokens.Primary900,
    outlineVariant   = ColorTokens.Primary200,

    // ===== Error =====
    error   = ColorTokens.Error100,
    onError = ColorTokens.White,
)
