package com.jehadalomour.flowvan.core.data.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Observes device connectivity. [onlineEvents] emits once each time the network becomes
 * available again — used to retry pending uploads as soon as the internet returns.
 */
expect class ConnectivityObserver {
    fun isOnline(): Boolean
    val onlineEvents: Flow<Unit>
}
