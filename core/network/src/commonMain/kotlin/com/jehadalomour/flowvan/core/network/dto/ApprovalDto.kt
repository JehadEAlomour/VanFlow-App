package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/** F10 approval-request types — mirrors the backend enum. */
object ApprovalTypes {
    const val RETURN_VOUCHER = "RETURN_VOUCHER"
    const val VOUCHER_DISCOUNT = "VOUCHER_DISCOUNT"
    const val PRICE_OVERRIDE = "PRICE_OVERRIDE"
}

@Serializable
data class CreateApprovalRequest(
    val type: String,
    /** The proposed voucher, exactly as it would have been posted. */
    val payload: CreateVoucherRequest,
    val note: String? = null,
    val customerNumber: String? = null,
)

@Serializable
data class ApprovalRequestDto(
    val id: String,
    val type: String,
    val status: String,                 // pending | approved | rejected | cancelled
    val customerNumber: String? = null,
    val note: String? = null,
    val decisionNote: String? = null,
    val resultVoucher: String? = null,
    val createdAt: String = "",
    val decidedAt: String? = null,
)
