package com.jehadalomour.flowvan.shared.domain.model

import kotlinx.serialization.Serializable

enum class InvoiceType { SALE, RETURN, REQUEST }

enum class InvoiceStatus { DRAFT, CONFIRMED, CANCELLED, FULFILLED }

@Serializable
data class InvoiceLine(
    val productId: String,
    val sku: String,
    val nameAr: String,
    val qty: Double,
    val unitPrice: Double,
    val discountPct: Double = 0.0,
    val lineTotal: Double,
)

data class Invoice(
    val id: String,
    val number: String,
    val type: InvoiceType,
    val status: InvoiceStatus,
    val customerId: String,
    val salesmanId: String,
    val createdAt: Long,
    val lines: List<InvoiceLine>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: PaymentMethod?,
    val notes: String?,
)