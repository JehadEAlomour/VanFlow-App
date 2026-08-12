package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The app theme. **Light only, and not configurable.**
 *
 * `darkTheme` is gone rather than defaulted to false: a parameter invites a call
 * site to pass true, and the palette, the print screens and every contrast
 * figure in this app assume a light ground. These reps work mornings in direct
 * sun, where a dark interface cannot be read at all.
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val fontFamily = almaraiFamily()
    val extended = LightExtendedColors
    val scheme = LightColorScheme

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