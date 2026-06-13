package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceDto(
    val id: String,
    val invoiceNumber: String = "",
    val status: String = "draft",
    val jofotaraStatus: String = "",
    val customerId: String = "",
    val repId: String = "",
    val createdAt: String? = null,
    val subtotal: Long = 0,
    val totalLineDiscounts: Long = 0,
    val invoiceDiscountAmount: Long = 0,
    val netTaxable: Long = 0,
    val netInclusive: Long = 0,
    val netExempt: Long = 0,
    val taxOnTaxable: Long = 0,
    val taxExtractedFromInclusive: Long = 0,
    val totalTax: Long = 0,
    val grandTotal: Long = 0,
    val lines: List<InvoiceLineDto> = emptyList(),
)

@Serializable
data class InvoiceLineDto(
    val id: String = "",
    val productId: String = "",
    val sku: String = "",
    val nameAr: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Long = 0,             // fils
    val lineTotal: Long = 0,             // fils
    val taxAmount: Long = 0,             // fils
)

@Serializable
data class CreateInvoiceRequest(
    val customerId: String,
    val repId: String,
    val lines: List<CreateInvoiceLine>,
    val invoiceDiscountType: String? = null,     // PERCENTAGE | FIXED_AMOUNT
    val invoiceDiscountValue: Double? = null,
    val paymentMethodCode: String? = null,        // 012 cash | 022 receivable
    val note: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class CreateInvoiceLine(
    val productId: String,
    val quantity: Double,
    val unitPrice: Long? = null,                  // fils; defaults to product.price
    val lineDiscountType: String? = null,
    val lineDiscountValue: Double? = null,
)

@Serializable
data class ReturnableLineDto(
    val invoiceLineId: String,
    val productId: String,
    val originalQty: Double,
    val returnableQty: Double,
)
