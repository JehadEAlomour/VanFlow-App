package com.jehadalomour.flowvan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jehadalomour.flowvan.core.designsystem.theme.AppTheme
import com.jehadalomour.flowvan.navigation.FlowVanNavHost

@Composable
fun App() {
    AppTheme(darkTheme = false) {
        // Outer box fills the full window (behind status bar + nav bar) with the app background.
        // Inner box pads content into the safe area so nothing overlaps system bars.
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
