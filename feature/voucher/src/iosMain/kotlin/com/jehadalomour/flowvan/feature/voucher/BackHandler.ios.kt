package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button — no-op
}
