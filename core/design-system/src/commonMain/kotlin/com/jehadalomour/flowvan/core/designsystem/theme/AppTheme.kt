package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val fontFamily = FontFamily.Default
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalExtendedTypography provides extendedTypography(fontFamily),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = appTypography(fontFamily),
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current

    val extendedTypography: ExtendedTypography
        @Composable
        get() = LocalExtendedTypography.current
}