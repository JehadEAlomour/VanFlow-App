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

    /**
     * Whether this salesman's new customers are created immediately, or wait for
     * the office to approve them (permissions.canCreateCustomerDirect).
     *
     * Advisory only: it exists so the create screen can say what will happen
     * BEFORE the rep fills a form and uploads a document photo. The server owns
     * the decision and reports it in the response either way, so a stale copy
     * here misleads for one screen — never creates a customer that should have
     * been reviewed. Defaults to false, so the warning shows when we do not know.
     */
    var canCreateCustomerDirect: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_CREATE_CUSTOMER_DIRECT, false)
        set(value) = settings.putBoolean(SettingsKeys.CAN_CREATE_CUSTOMER_DIRECT, value)

    /**
     * Whether this salesman's printed receipt shows the discount on each row
     * (permissions.canPrintLineDiscount). Persisted like canAddCustomer so a
     * receipt printed offline still honours the permission.
     */
    var canPrintLineDiscount: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_PRINT_LINE_DISCOUNT, false)
        set(value) = settings.putBoolean(SettingsKeys.CAN_PRINT_LINE_DISCOUNT, value)

    /** Whether the Find Customers screen is shown (permissions.canFindCustomers). */
    var canFindCustomers: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_FIND_CUSTOMERS, false)
        set(value) = settings.putBoolean(SettingsKeys.CAN_FIND_CUSTOMERS, value)

    /** Route-only: hide the Customers list on home (permissions.routesOnly). */
    var routesOnly: Boolean
        get() = settings.getBoolean(SettingsKeys.ROUTES_ONLY, false)
        set(value) = settings.putBoolean(SettingsKeys.ROUTES_ONLY, value)

    /**
     * The three customer-dashboard action gates (permissions.canCreateSale /
     * canCreateReturn / canMakeCollection). Selling, returning and collecting were
     * always allowed, so these DEFAULT TRUE (opt-out): the tile hides only when the
     * office has explicitly switched the permission off. Login writes each from the
     * server's permissions map, treating a missing key as allowed.
     */
    var canCreateSale: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_CREATE_SALE, true)
        set(value) = settings.putBoolean(SettingsKeys.CAN_CREATE_SALE, value)

    var canCreateReturn: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_CREATE_RETURN, true)
        set(value) = settings.putBoolean(SettingsKeys.CAN_CREATE_RETURN, value)

    var canMakeCollection: Boolean
        get() = settings.getBoolean(SettingsKeys.CAN_MAKE_COLLECTION, true)
        set(value) = settings.putBoolean(SettingsKeys.CAN_MAKE_COLLECTION, value)

    /** Max direct-discount % encoded as "vouchers.discount.max:<n>", or null = uncapped. */
    fun discountMaxPct(): Double? =
        currentPermKeys.orEmpty().split(',')
            .firstOrNull { it.trim().startsWith("vouchers.discount.max:") }
            ?.substringAfter(':')?.toDoubleOrNull()

    /**
     * Long-lived, location-only credential issued at sign-in.
     *
     * Deliberately outside the session: the handset must keep reporting its
     * position after the salesman signs out, so [clear] must not touch this.
     * Only the office releasing the device revokes it — the server refuses it
     * from then on, and [trackingRepId] is what the uploader posts against.
     */
    var trackingToken: String?
        get() = settings.getStringOrNull(SettingsKeys.TRACKING_TOKEN)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.TRACKING_TOKEN)
            else settings.putString(SettingsKeys.TRACKING_TOKEN, value)
        }

    /** Rep the tracking token belongs to — survives sign-out alongside it. */
    var trackingRepId: String?
        get() = settings.getStringOrNull(SettingsKeys.TRACKING_REP_ID)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.TRACKING_REP_ID)
            else settings.putString(SettingsKeys.TRACKING_REP_ID, value)
        }

    /** Handset id reported at the last sign-in, echoed back on sign-out. */
    var boundDeviceId: String?
        get() = settings.getStringOrNull(SettingsKeys.BOUND_DEVICE_ID)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.BOUND_DEVICE_ID)
            else settings.putString(SettingsKeys.BOUND_DEVICE_ID, value)
        }

    /**
     * Ends the interactive session only.
     *
     * The tracking credential and the device binding are left alone — that is
     * what makes "signed out but still tracking" work. Wiping them here would
     * silently kill the trail the office depends on.
     */
    fun clear() {
        settings.remove(SettingsKeys.CURRENT_USER_ID)
        settings.remove(SettingsKeys.CURRENT_TOKEN)
        settings.remove(SettingsKeys.CURRENT_REP_ID)
        settings.remove(SettingsKeys.CURRENT_USER_CODE)
        settings.remove(SettingsKeys.CURRENT_PERM_KEYS)
        settings.remove(SettingsKeys.CAN_ADD_CUSTOMER)
        settings.remove(SettingsKeys.CAN_FIND_CUSTOMERS)
        settings.remove(SettingsKeys.ROUTES_ONLY)
    }

    /** Forgets the tracking credential too — only for a released/reset device. */
    fun clearTracking() {
        settings.remove(SettingsKeys.TRACKING_TOKEN)
        settings.remove(SettingsKeys.TRACKING_REP_ID)
    }
}