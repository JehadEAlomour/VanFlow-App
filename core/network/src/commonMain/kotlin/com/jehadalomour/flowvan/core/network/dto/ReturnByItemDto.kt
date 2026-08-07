package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Return-by-item wire types. Mirror of the backend's
 * `src/modules/vouchers/returns/` DTOs — see
 * cash-van-dashboard/docs/RETURNS-without-a-sale-voucher.md.
 */

@Serializable
data class ReturnRequestLineDto(
    val itemNumber: String,
    /**
     * The unit coming back. Sent whenever the item has unit rows: a carton must
     * never be matched against a piece line, and the allocator keys on the pair.
     */
    val itemUnitId: String? = null,
    val quantity: Double,
    /** Only read by CLOSEST_PRICE, which the app does not offer. */
    val expectedUnitPrice: Double? = null,
)

@Serializable
data class ReturnPreviewRequest(
    val lines: List<ReturnRequestLineDto>,
    /**
     * The app always sends NEWEST_FIRST and offers no picker. A salesman in a
     * shop should not be choosing an allocation policy; the office can re-do a
     * return under a different order if the match is wrong.
     */
    val strategy: String = "NEWEST_FIRST",
    val customerNumber: String? = null,
    val userCode: String? = null,
)

@Serializable
data class ConfirmReturnRequest(
    val lines: List<ReturnRequestLineDto>,
    val strategy: String = "NEWEST_FIRST",
    val customerNumber: String? = null,
    val userCode: String? = null,
    val confirmUserCode: String,
    val storeNumber: String? = null,
    /** Post immediately — the van's returns are a completed act at the counter. */
    val post: Boolean = true,
)

/** One unit-run taken from a single past sale. */
@Serializable
data class AllocatedReturnLineDto(
    val voucherNumber: String,
    val lineId: String,
    val itemNumber: String,
    val itemName: String,
    val itemUnitId: String? = null,
    val unitCode: String? = null,
    val unitName: String? = null,
    val customerNumber: String? = null,
    val inDate: String,
    val quantity: Double,
    val unitPrice: Double,
    val discountValue: Double = 0.0,
    val taxValue: Double = 0.0,
    val netTotal: Double = 0.0,
)

@Serializable
data class UnallocatedReturnLineDto(
    val itemNumber: String,
    val itemUnitId: String? = null,
    val quantity: Double,
    val reason: String,
)

@Serializable
data class ReturnPlanDto(
    val strategy: String = "NEWEST_FIRST",
    val lines: List<AllocatedReturnLineDto> = emptyList(),
    val unallocated: List<UnallocatedReturnLineDto> = emptyList(),
    /** How many RETURN vouchers a confirm will create — one per source sale. */
    val voucherCount: Int = 0,
    val refundTotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val error: String? = null,
) {
    @Serializable
    data class Created(val vouchers: List<String> = emptyList())
}

@Serializable
data class ReturnPreviewResponse(
    val plan: ReturnPlanDto,
)
