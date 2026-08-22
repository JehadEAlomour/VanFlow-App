package com.jehadalomour.flowvan.core.datastore

/**
 * Persisted [com.russhwolf.settings.Settings] keys.
 *
 * The string values are the on-disk keys — changing one drops any value already saved
 * under the old key, so treat them as stable.
 */
object SettingsKeys {

    // ── App / first-run ─────────────────────────────────────────────
    /** Selected UI language (AppLanguage.name). */
    const val APP_LANGUAGE = "app_language"

    // ── Session (set at login, cleared on logout) ───────────────────
    /** Logged-in user id — drives session restore on relaunch. */
    const val CURRENT_USER_ID = "current_user_id"

    /** Bearer JWT sent as Authorization on every backend call. */
    const val CURRENT_TOKEN = "current_token"

    /** Backend rep id (distinct from user id) — required by invoice/collection endpoints. */
    const val CURRENT_REP_ID = "current_rep_id"

    /** Backend user code (userNumber) — required by the voucher API's userCode. */
    const val CURRENT_USER_CODE = "current_user_code"

    /** F10 permission keys (comma-joined) — gate returns/discounts/price edits. */
    const val CURRENT_PERM_KEYS = "current_perm_keys"

    /** Whether the signed-in user may create customers (permissions.canAddCustomer). */
    const val CAN_ADD_CUSTOMER = "can_add_customer"

    /**
     * Whether a customer this salesman adds is REAL immediately, or waits for the
     * office (permissions.canCreateCustomerDirect). Only used to warn before the
     * save — the server decides, and says so in its answer.
     */
    const val CAN_CREATE_CUSTOMER_DIRECT = "can_create_customer_direct"

    /** Print the discount on each receipt row (permissions.canPrintLineDiscount). */
    const val CAN_PRINT_LINE_DISCOUNT = "can_print_line_discount"

    /** Show the Find Customers screen (permissions.canFindCustomers). */
    const val CAN_FIND_CUSTOMERS = "can_find_customers"

    /** Hide the Customers list; reach customers via the route only (permissions.routesOnly). */
    const val ROUTES_ONLY = "routes_only"

    // ── Device binding (survives sign-out on purpose) ───────────────
    /** Long-lived, location-only credential. NOT cleared by SessionStore.clear(). */
    const val TRACKING_TOKEN = "tracking_token"

    /** Handset id this install reported at its last sign-in. */
    const val BOUND_DEVICE_ID = "bound_device_id"

    /** Rep the tracking token belongs to — outlives the session with it. */
    const val TRACKING_REP_ID = "tracking_rep_id"

    // ── Backend (VanFlow API) ───────────────────────────────────────
    /** Base URL of the VanFlow backend, e.g. ".../api/v1" (see ApiConfig). */
    const val API_BASE_URL = "api_base_url"

    /** Legacy P6 sync-engine endpoint base (SyncConfig/SyncApi batch upload). */
    const val SYNC_BASE_URL = "sync_base_url"

    // ── AI ──────────────────────────────────────────────────────────
    /** Claude API key for the AI assistant (see AiSettings). */
    const val AI_API_KEY = "ai_api_key"
}