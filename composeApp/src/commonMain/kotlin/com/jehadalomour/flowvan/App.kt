package com.jehadalomour.flowvan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.jehadalomour.flowvan.core.designsystem.theme.AppTheme
import com.jehadalomour.flowvan.navigation.FlowVanNavHost

@Composable
fun App() {
    AppTheme(darkTheme = false) {
        // Lay the UI out LTR (back button on the leading-left, actions on the right),
        // matching iOS. Android otherwise forces RTL from the Arabic locale, which
        // mirrors every top bar to the wrong side. Arabic text still shapes
        // right-to-left within each label — only the box/row layout is affected.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF4F6FB)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    FlowVanNavHost()
                }
            }
        }
    }
}
