package com.jehadalomour.flowvan

import androidx.compose.runtime.Composable
import com.jehadalomour.flowvan.core.designsystem.theme.AppTheme
import com.jehadalomour.flowvan.navigation.FlowVanNavHost

@Composable
fun App() {
    AppTheme(darkTheme = false) {
        FlowVanNavHost()
    }
}
