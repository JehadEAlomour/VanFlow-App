package com.jehadalomour.flowvan.core.model

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
    val taxType: String = "TAXABLE",    // LineTaxType.name — defaults to TAXABLE for old records
    val taxAmount: Double = 0.0,        // per-line tax (before invoice discount)
    val unit: String = "",              // unit name (حبة, كرتونة, etc.)
    /**
     * `item_units.id` of the chosen unit ("" = base pool). Has a default because unsynced
     * invoices already sitting in `linesJson` on a rep's phone are deserialized after the
     * update — without it they would fail to parse and never upload.
     */
    val unitId: String = "",
    val unitConversionQty: Double = 1.0, // pieces per unit (base = 1) — for stock + ERP per-piece
    val taxRate: Double = 0.0,          // per-product rate used when the line was created
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
    /** JoFotara e-invoice QR content, mirrored from the server once the ERP
     * submits the sale to the government. Null until it lands (async); the
     * receipt shows the QR when present and a "pending" note otherwise. */
    val jofotaraQrCode: String? = null,
)