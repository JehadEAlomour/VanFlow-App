package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/** `POST /vouchers` body. Monetary/qty fields are numeric strings ("1.250", "2.000"). */
@Serializable
data class CreateVoucherRequest(
    val voucherNumber: String,
    val transKind: String,                 // SALE | RETURN | ORDER
    val userCode: String,
    val customerNumber: String? = null,
    val vendorNumber: String? = null,
    val inDate: String? = null,
    val totalDiscountValue: String? = null,
    val totalDiscountPercentage: String? = null,
    val isPosted: Boolean = true,
    val transactions: List<VoucherTxn>,
    val payments: List<VoucherPayment> = emptyList(),
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

@Serializable
data class VoucherKindRequest(
    val transKind: String,
    val transName: String,
    val sign: Int = 0,
)
