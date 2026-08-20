package com.jehadalomour.flowvan.feature.prospecting

import com.jehadalomour.flowvan.core.network.dto.ProspectDto

/** A found shop with its distance from the rep, ready to render. */
data class NearbyProspect(
    val prospect: ProspectDto,
    /** Metres from the rep's current position; null when the shop has no fix. */
    val distanceM: Int?,
) {
    val isExistingCustomer: Boolean get() = prospect.isExistingCustomer
}

data class FindCustomersState(
    // ── Location ──────────────────────────────────────────────────────────────
    /** The rep's position; the whole search hangs off it, so nothing runs until it exists. */
    val lat: Double? = null,
    val lng: Double? = null,
    val isLocating: Boolean = false,
    /** No GPS fix — shown instead of searching from a wrong or default point. */
    val locationError: Boolean = false,

    // ── Category picker ───────────────────────────────────────────────────────
    val featuredCategories: List<String> = emptyList(),
    val selectedCategories: Set<String> = emptySet(),
    /** The rep's own words, added when the featured chips have nothing that fits. */
    val keywords: List<String> = emptyList(),

    // ── Results ───────────────────────────────────────────────────────────────
    val isSearching: Boolean = false,
    val results: List<NearbyProspect> = emptyList(),
    /** True once a search has returned, so an empty list reads as "none found" not "not searched yet". */
    val hasSearched: Boolean = false,
    val errorAr: String? = null,
) {
    /** Fixed 2 km, per the product decision — no radius control on the screen. */
    val radiusM: Int get() = 2_000

    val hasLocation: Boolean get() = lat != null && lng != null

    /**
     * The server needs a category or a keyword — a bare point is a 400. The
     * button stays disabled until the rep has picked at least one of either.
     */
    val canSearch: Boolean
        get() = hasLocation && !isSearching &&
            (selectedCategories.isNotEmpty() || keywords.isNotEmpty())
}

sealed interface FindCustomersEvent {
    /** Retry the GPS fix after a denial or a cold start. */
    data object RequestLocation : FindCustomersEvent
    data class ToggleCategory(val key: String) : FindCustomersEvent
    data class AddKeyword(val term: String) : FindCustomersEvent
    data class RemoveKeyword(val term: String) : FindCustomersEvent
    data object Search : FindCustomersEvent
    data object DismissError : FindCustomersEvent
}
