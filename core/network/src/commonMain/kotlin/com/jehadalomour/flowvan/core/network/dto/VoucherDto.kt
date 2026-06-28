package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `POST /sync/vouchers` body. Monetary/qty fields are numeric strings ("1.250").
 * `clientRef` is the device's local id — the server dedupes replays by it and
 * assigns the authoritative voucher number, so client numbers never collide.
 */
@Serializable
data class CreateVoucherRequest(
    // Nullable so the approval flow can OMIT it (null → dropped by the request
    // encoder's explicitNulls=false) and let the server assign a unique serial.
    // The backend rejects an empty string (@Length(1,64)); only omission is valid.
    val voucherNumber: String? = null,
    val transKind: String,                 // SALE | RETURN | ORDER
    val userCode: String,
    val clientRef: String? = null,
    /** RETURN: the original SALE voucher number this return is issued against. */
    val referenceVoucherNumber: String? = null,
    val customerNumber: String? = null,
    val vendorNumber: String? = null,
    val inDate: String? = null,
    val totalDiscountValue: String? = null,
    val totalDiscountPercentage: String? = null,
    val isPosted: Boolean = true,
    val transactions: List<VoucherTxn>,
    val payments: List<VoucherPayment> = emptyList(),
    /**
     * GIFT picks for ITEM_QTY_REWARD offers (item numbers). The server validates them
     * against the offer's gift pool, adds them as free lines, and records the redemption.
     */
    val chosenFreeItems: List<String> = emptyList(),
)

/** `POST /sync/vouchers` response: server-assigned number + staging status. */
@Serializable
data class SyncVoucherResult(
    val id: String = "",
    val voucherNumber: String = "",
    val status: String = "",               // pending | posted | failed
    val error: String? = null,
)

@Serializable
data class VoucherTxn(
    val itemNumber: String,
    val itemName: String,
    val itemQty: String,
    val unitPrice: String,
    val taxPercentage: String = "0",
    val discountPercentage: String = "0",
    val discountValue: String? = null,
    val storeNumber: String? = null,
    val transKind: String? = null,         // defaults to the header transKind
    // The chosen sales unit for this line. unitBaseQty = pieces per unit, used
    // server-side for stock movement (itemQty × unitBaseQty) and ERP per-piece price.
    val unitCode: String? = null,
    val unitName: String? = null,
    val unitBaseQty: Int? = null,
)

@Serializable
data class VoucherPayment(
    val amount: String,
    val paymentDate: String? = null,
    val fromAcc: String? = null,
    val toAcc: String? = null,
    val paymentType: String,               // CASH | CHEQUE | TRANSFER | CARD | CREDIT
)

@Serializable
data class VoucherDto(
    val id: String,
    val voucherNumber: String = "",
    val transKind: String = "",
    val isPosted: Boolean = false,
)

/** A server voucher header in a list (used to pick a RETURN's source SALE). */
@Serializable
data class VoucherSummaryDto(
    val id: String,
    val voucherNumber: String = "",
    val transKind: String = "",
    val customerNumber: String? = null,
    val inDate: String? = null,
    val createdAt: String? = null,
    val netTotal: String = "0",
    val isPosted: Boolean = false,
)

/** A server voucher with its lines (to pre-fill a return from the original sale). */
@Serializable
data class VoucherDetailDto(
    val id: String,
    val voucherNumber: String = "",
    val customerNumber: String? = null,
    val transactions: List<VoucherDetailTxn> = emptyList(),
)

@Serializable
data class VoucherDetailTxn(
    val itemNumber: String = "",
    val itemName: String = "",
    val itemQty: String = "0",
    val unitPrice: String = "0",
    val taxPercentage: String = "0",
    val discountPercentage: String = "0",
    val unitName: String? = null,
    val unitCode: String? = null,
    val unitBaseQty: Int? = null,
)

@Serializable
data class VoucherKindRequest(
    val transKind: String,
    val transName: String,
    val sign: Int = 0,
)
