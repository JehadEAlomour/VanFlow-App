package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Money read live from the ERP (the book of record). All amounts are MAJOR units
 * (JOD decimals) as the ERP serves them — NOT fils — so they map straight to the
 * app's Double JOD domain with no /1000.
 *
 * Every payload carries `source`: "erp" is a real figure; "unavailable" means the
 * hub couldn't produce one (reason: unlinked | erp_off | not_found | fetch_failed),
 * so the app shows a gap, never a guessed number.
 */
@Serializable
data class ErpBalanceDto(
    val source: String = "unavailable",
    val reason: String? = null,
    val balance: Double? = null,
    val creditLimit: Double? = null,
    val accountCode: String? = null,
    val accountName: String? = null,
)

/** One posting line of an ERP customer statement (major units). */
@Serializable
data class ErpStatementLineDto(
    val date: String? = null,
    val type: String = "",
    val reference: String = "",
    val description: String = "",
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val balance: Double = 0.0,
)

/**
 * The customer statement endpoint returns EITHER the full statement OR the
 * `{ source: "unavailable", reason }` envelope. Every field is optional so one
 * DTO decodes both shapes; [isAvailable] tells them apart.
 */
@Serializable
data class ErpStatementDto(
    val customerCode: String? = null,
    val customerName: String? = null,
    val creditLimit: Double? = null,
    val openingBalance: Double? = null,
    val closingBalance: Double? = null,
    val lines: List<ErpStatementLineDto> = emptyList(),
    // Present only on the unavailable envelope.
    val source: String? = null,
    val reason: String? = null,
) {
    /** True when this carries a real statement (not the unavailable envelope). */
    val isAvailable: Boolean get() = source != "unavailable" && openingBalance != null
}
