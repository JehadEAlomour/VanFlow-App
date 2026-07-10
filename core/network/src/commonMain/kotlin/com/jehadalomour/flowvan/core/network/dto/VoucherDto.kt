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
    /** Rep's GPS at sale time — lets the backend enforce the per-rep location lock
     *  (customers.requireProximity) and seed a missing customer pin. Omitted when null. */
    val repLat: Double? = null,
    val repLng: Double? = null,
    val transactions: List<VoucherTxn>,
    val payments: List<VoucherPayment> = emptyList(),
    /**
     * GIFT picks for ITEM_QTY_REWARD offers (item numbers). The server validates them
     * against the offer's gift pool, adds them as free lines, and records the redemption.
     */
    val chosenFreeItems: List<String> = emptyList(),
)

/**
 * `POST /sync/vouchers` response: the created voucher header. Beyond the server-assigned
 * number it carries the SERVER-COMPUTED invoice (money engine + offers already applied),
 * so the app can adopt the authoritative totals/lines on sync instead of trusting its own
 * on-device calc. All money fields are major-unit numeric strings ("8.120"). Numeric/nested
 * fields default so an older backend (number only) still deserializes.
 */
@Serializable
data class SyncVoucherResult(
    val id: String = "",
    val voucherNumber: String = "",
    val status: String = "",               // pending | posted | failed
    val error: String? = null,
    /** Grand total WITH tax (what the customer pays). */
    val netTotal: String = "0",
    val totalTax: String = "0",
    /** Header-level discount only (line discounts live on each transaction). */
    val totalDiscountValue: String = "0",
    val transactions: List<CreatedVoucherTxn> = emptyList(),
)

/** One server-computed line on a created voucher. Money = major-unit numeric strings. */
@Serializable
data class CreatedVoucherTxn(
    val itemNumber: String = "",
    val itemName: String = "",
    val itemQty: String = "0",
    val unitPrice: String = "0",
    val taxPercentage: String = "0",
    /** Resolved line discount (own line discount + its share of the header discount), JOD. */
    val discountValue: String = "0",
    val discountPercentage: String = "0",
    /** Line net (post-discount, tax base). Gift/free lines net to 0. */
    val total: String = "0",
    /** Line grand total WITH tax. */
    val netTotal: String = "0",
    val unitName: String? = null,
    val unitBaseQty: Int? = null,
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
