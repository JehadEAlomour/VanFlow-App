package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The customer's account as the ERP keeps it.
 *
 * WHY THIS EXISTS ALONGSIDE THE VOUCHER LEDGER
 * The statement the app used to build came from cash-van's own vouchers and
 * collections — what the vans did. A shop invoiced in the ERP office has none of
 * those, so its statement came back empty while its balance said it owed money.
 * The rep then had a screen that disagreed with the figure directly above it.
 *
 * The server answers `{ source: "unavailable", reason: ... }` instead of a
 * statement when the customer has no ERP code, when ERP mode is off, or when the
 * ERP could not be reached — so every field here is optional and the caller
 * checks [isUsable] rather than trusting the shape.
 */
@Serializable
data class ErpStatementDto(
    /** Present ONLY on the unavailable envelope. Null on a real statement. */
    val source: String? = null,
    /** unlinked | erp_off | not_found | fetch_failed — why there is no statement. */
    val reason: String? = null,
    val customerCode: String? = null,
    val customerName: String? = null,
    val from: String? = null,
    val to: String? = null,
    val openingBalance: Double? = null,
    val closingBalance: Double? = null,
    val lines: List<ErpStatementLineDto> = emptyList(),
) {
    /**
     * A statement we can actually show. `source` is only set on the failure
     * envelope, so its absence is what marks a real answer — an empty `lines` on a
     * real statement is a genuine "nothing happened in this period", not a failure,
     * and must not be mistaken for one.
     */
    val isUsable: Boolean get() = source == null
}

/** One posting line. Money is major units, as the ERP renders it. */
@Serializable
data class ErpStatementLineDto(
    /** ISO date, or null on a line the ERP could not date. */
    val date: String? = null,
    /** INVOICE | PAYMENT. */
    val type: String = "",
    /** The ERP's own document number — NOT a cash-van voucher number. */
    @SerialName("reference") val reference: String = "",
    val description: String = "",
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val balance: Double = 0.0,
)
