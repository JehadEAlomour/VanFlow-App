package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A candidate business found near the rep. Mirrors the backend `Prospect`.
 *
 * `lat`/`lng` arrive as strings because the column is numeric(9,6) — sending
 * them as doubles would round the sixth decimal, which is about 10 cm and
 * enough to move a shopfront across a narrow street.
 */
@Serializable
data class ProspectDto(
    val id: String,
    val name: String,
    val lat: String? = null,
    val lng: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val category: String? = null,
    val rating: String? = null,
    val status: String = "NEW",
    /** Set when de-dup decided this is already a customer — do not offer to add it. */
    @SerialName("matchedCustomerId") val matchedCustomerId: String? = null,
    @SerialName("matchReason") val matchReason: String? = null,
) {
    val latitude: Double? get() = lat?.toDoubleOrNull()
    val longitude: Double? get() = lng?.toDoubleOrNull()

    /** Already a customer: the row is shown, but as information, not a lead. */
    val isExistingCustomer: Boolean get() = matchedCustomerId != null
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

/** What one search run recorded. */
@Serializable
data class ProspectSearchDto(
    val id: String,
    @SerialName("foundCount") val foundCount: Int = 0,
    @SerialName("newCount") val newCount: Int = 0,
)

/** POST /prospecting/searches. */
@Serializable
data class CreateProspectSearchDto(
    val lat: Double,
    val lng: Double,
    @SerialName("radiusM") val radiusM: Int,
    val categories: List<String>? = null,
    /** The rep's own words, when the category list has nothing that fits. */
    val keywords: List<String>? = null,
)

/** The searchable category allow-list plus the short one-tap subset. */
@Serializable
data class ProspectCategoriesDto(
    val all: List<String> = emptyList(),
    val featured: List<String> = emptyList(),
)
