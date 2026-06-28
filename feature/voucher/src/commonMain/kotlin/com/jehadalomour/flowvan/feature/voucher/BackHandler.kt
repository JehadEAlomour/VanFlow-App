package com.jehadalomour.flowvan.feature.voucher

import androidx.compose.runtime.Composable

@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)
