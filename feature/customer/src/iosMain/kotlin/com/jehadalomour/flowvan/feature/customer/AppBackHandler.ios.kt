package com.jehadalomour.flowvan.feature.customer

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button — no-op
}
