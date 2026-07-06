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

    // ── Backend (VanFlow API) ───────────────────────────────────────
    /** Base URL of the VanFlow backend, e.g. ".../api/v1" (see ApiConfig). */
    const val API_BASE_URL = "api_base_url"

    /** Legacy P6 sync-engine endpoint base (SyncConfig/SyncApi batch upload). */
    const val SYNC_BASE_URL = "sync_base_url"

    // ── AI ──────────────────────────────────────────────────────────
    /** Claude API key for the AI assistant (see AiSettings). */
    const val AI_API_KEY = "ai_api_key"
}