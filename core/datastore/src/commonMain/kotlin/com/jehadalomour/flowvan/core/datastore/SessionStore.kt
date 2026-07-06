package com.jehadalomour.flowvan.core.datastore

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionStore(private val settings: Settings) {

    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits whenever a 401 forces a session clear — observe to redirect to login. */
    val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    fun signalUnauthorized() {
        clear()
        _unauthorizedEvents.tryEmit(Unit)
    }

    var currentUserId: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_USER_ID)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_USER_ID)
            else settings.putString(SettingsKeys.CURRENT_USER_ID, value)
        }

    var currentToken: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_TOKEN)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_TOKEN)
            else settings.putString(SettingsKeys.CURRENT_TOKEN, value)
        }

    /** Backend rep id (distinct from the user id) — required by invoice/collection endpoints. */
    var currentRepId: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_REP_ID)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_REP_ID)
            else settings.putString(SettingsKeys.CURRENT_REP_ID, value)
        }

    /** Backend user code (userNumber) — required by the voucher API's userCode. */
    var currentUserCode: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_USER_CODE)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_USER_CODE)
            else settings.putString(SettingsKeys.CURRENT_USER_CODE, value)
        }

    /** F10 permission keys, comma-joined (e.g. "vouchers.discount.direct,vouchers.discount.max:5"). */
    var currentPermKeys: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_PERM_KEYS)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_PERM_KEYS)
            else settings.putString(SettingsKeys.CURRENT_PERM_KEYS, value)
        }

    /** True when the signed-in salesman holds the given F10 permission key. */
    fun can(key: String): Boolean =
        currentPermKeys.orEmpty().split(',').any { it.trim() == key }

    /** Whether the signed-in user may create customers (permissions.canAddCustomer). */
    var canAddCustomer: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_ADD_CUSTOMER, false)
        set(value) = settings.putBoolean(SettingsKeys.CAN_ADD_CUSTOMER, value)

    /** Max direct-discount % encoded as "vouchers.discount.max:<n>", or null = uncapped. */
    fun discountMaxPct(): Double? =
        currentPermKeys.orEmpty().split(',')
            .firstOrNull { it.trim().startsWith("vouchers.discount.max:") }
            ?.substringAfter(':')?.toDoubleOrNull()

    fun clear() {
        settings.remove(SettingsKeys.CURRENT_USER_ID)
        settings.remove(SettingsKeys.CURRENT_TOKEN)
        settings.remove(SettingsKeys.CURRENT_REP_ID)
        settings.remove(SettingsKeys.CURRENT_USER_CODE)
        settings.remove(SettingsKeys.CURRENT_PERM_KEYS)
        settings.remove(SettingsKeys.CAN_ADD_CUSTOMER)
    }
}