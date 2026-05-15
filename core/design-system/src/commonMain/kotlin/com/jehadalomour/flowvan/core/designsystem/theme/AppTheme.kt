package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val fontFamily = FontFamily.Default

    CompositionLocalProvider(
        LocalExtendedColors provides LightExtendedColors,
        LocalExtendedTypography provides extendedTypography(fontFamily),
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
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
