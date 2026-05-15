package com.jehadalomour.flowvan.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class ExtendedTypography(
    // Display
    val displayLarge: TextStyle,
    val displayLargeEmphasized: TextStyle,
    val displayMedium: TextStyle,
    val displayMediumEmphasized: TextStyle,
    val displaySmall: TextStyle,
    val displaySmallEmphasized: TextStyle,
    val displayXSmall: TextStyle,
    val displayXSmallEmphasized: TextStyle,
    // Headline
    val headlineLarge: TextStyle,
    val headlineLargeEmphasized: TextStyle,
    val headlineMedium: TextStyle,
    val headlineMediumEmphasized: TextStyle,
    val headlineSmall: TextStyle,
    val headlineSmallEmphasized: TextStyle,
    val headlineXSmall: TextStyle,
    val headlineXSmallEmphasized: TextStyle,
    // Title
    val titleLarge: TextStyle,
    val titleLargeEmphasized: TextStyle,
    val titleMedium: TextStyle,
    val titleMediumEmphasized: TextStyle,
    val titleSmall: TextStyle,
    val titleSmallEmphasized: TextStyle,
    val titleXSmall: TextStyle,
    val titleXSmallEmphasized: TextStyle,
    // Label
    val labelLarge: TextStyle,
    val labelLargeEmphasized: TextStyle,
    val labelMedium: TextStyle,
    val labelMediumEmphasized: TextStyle,
    val labelSmall: TextStyle,
    val labelSmallEmphasized: TextStyle,
    val labelXSmall: TextStyle,
    val labelXSmallEmphasized: TextStyle,
    // Body
    val bodyLarge: TextStyle,
    val bodyLargeEmphasized: TextStyle,
    val bodyMedium: TextStyle,
    val bodyMediumEmphasized: TextStyle,
    val bodySmall: TextStyle,
    val bodySmallEmphasized: TextStyle,
    val bodyXSmall: TextStyle,
    val bodyXSmallEmphasized: TextStyle,
)

val LocalExtendedTypography = staticCompositionLocalOf<ExtendedTypography> {
    error("ExtendedTypography not provided — wrap your content with AppTheme")
}

fun extendedTypography(fontFamily: FontFamily) = ExtendedTypography(
    /* Display */
    displayLarge            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayLargeEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displayMediumEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmallEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp),
    displayXSmall           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 8.sp,  lineHeight = 12.sp),
    displayXSmallEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 8.sp,  lineHeight = 12.sp),
    /* Headline */
    headlineLarge            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLargeEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMediumEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmallEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 24.sp, lineHeight = 32.sp),
    headlineXSmall           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 8.sp,  lineHeight = 12.sp),
    headlineXSmallEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 8.sp,  lineHeight = 12.sp),
    /* Title */
    titleLarge            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleLargeEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    titleMediumEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmallEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 14.sp, lineHeight = 20.sp),
    titleXSmall           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 8.sp,  lineHeight = 12.sp),
    titleXSmallEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 8.sp,  lineHeight = 12.sp),
    /* Label */
    labelLarge            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLargeEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelMediumEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    labelSmallEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 11.sp, lineHeight = 16.sp),
    labelXSmall           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 8.sp,  lineHeight = 12.sp),
    labelXSmallEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 8.sp,  lineHeight = 12.sp),
    /* Body */
    bodyLarge            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLargeEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMediumEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall            = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    bodySmallEmphasized  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 12.sp, lineHeight = 16.sp),
    bodyXSmall           = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 8.sp,  lineHeight = 12.sp),
    bodyXSmallEmphasized = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = 8.sp,  lineHeight = 12.sp),
)

fun appTypography(fontFamily: FontFamily) = Typography(
    displayLarge  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),

    headlineLarge  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),

    titleLarge  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

    bodyLarge  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
