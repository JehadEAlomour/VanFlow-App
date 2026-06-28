package com.jehadalomour.flowvan.core.data.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS connectivity. Reconnect events are not wired through NWPathMonitor here, so iOS relies on
 * the periodic sync poll + on-create + home-refresh triggers to retry pending uploads.
 */
actual class ConnectivityObserver {
    actual fun isOnline(): Boolean = true
    actual val onlineEvents: Flow<Unit> = emptyFlow()
}
