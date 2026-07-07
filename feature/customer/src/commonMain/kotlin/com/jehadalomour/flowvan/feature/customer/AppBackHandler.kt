package com.jehadalomour.flowvan.feature.customer

import androidx.compose.runtime.Composable

/** Intercept the platform back gesture/button. Android → system BackHandler; iOS → no-op. */
@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)
