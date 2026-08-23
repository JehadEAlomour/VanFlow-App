package com.jehadalomour.flowvan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.network.api.NotificationApi
import com.jehadalomour.flowvan.core.network.http.NetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The rep's notification inbox. Polled, not pushed — the handsets are GMS-less,
 * so this fetches on open and whenever the screen resumes.
 */
class NotificationsViewModel(
    private val api: NotificationApi,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init { refresh() }

    fun onEvent(e: NotificationsEvent) {
        when (e) {
            NotificationsEvent.Refresh -> refresh()
            is NotificationsEvent.MarkRead -> markRead(e.id)
            NotificationsEvent.MarkAllRead -> markAllRead()
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val res = api.list(unreadOnly = false, limit = 50)
                _state.update { it.copy(isLoading = false, items = res.items, unread = res.unread) }
            } catch (e: NetworkException) {
                _state.update { it.copy(isLoading = false, error = e.error.messageAr) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun markRead(id: String) {
        // Optimistic: flip locally, drop the unread count, then tell the server.
        _state.update { s ->
            val already = s.items.firstOrNull { it.id == id }?.isUnread == false
            s.copy(
                items = s.items.map { if (it.id == id) it.copy(readAt = it.readAt ?: "read") else it },
                unread = if (already) s.unread else (s.unread - 1).coerceAtLeast(0),
            )
        }
        viewModelScope.launch { runCatching { api.markRead(id) } }
    }

    private fun markAllRead() {
        _state.update { s ->
            s.copy(items = s.items.map { it.copy(readAt = it.readAt ?: "read") }, unread = 0)
        }
        viewModelScope.launch { runCatching { api.markAllRead() } }
    }
}
