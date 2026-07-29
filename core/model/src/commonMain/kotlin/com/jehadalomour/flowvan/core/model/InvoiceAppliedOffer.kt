package com.jehadalomour.flowvan.core.model

import kotlinx.serialization.Serializable

/**
 * One offer applied to a saved invoice, frozen for the printed receipt: the offer's display
 * name and the total discount value (JOD, major units) it contributed across all lines/gifts.
 *
 * Persisted on the invoice (as a JSON list) at sale time because the live offer evaluation —
 * which knows the per-offer amounts — isn't available later when printing from a report. The
 * printed footer renders one row per entry plus a "total offer discount" row.
 */
@Serializable
data class InvoiceAppliedOffer(
    val name: String,
    /** Discount value this offer contributed, in JOD (major units). */
    val discountAmount: Double,
)
