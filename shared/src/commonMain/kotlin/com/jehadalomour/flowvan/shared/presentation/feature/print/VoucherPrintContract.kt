package com.jehadalomour.flowvan.shared.presentation.feature.print

import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine

data class VoucherPrintState(
    val isLoading: Boolean = true,
    val invoiceId: String = "",
    val number: String = "",
    val type: String = "SALE",
    val paymentMethod: String? = null,
    val createdAt: Long = 0L,
    val customerNameAr: String = "",
    val customerCode: String = "",
    val customerTaxNumber: String? = null,
    val salesmanNameAr: String = "",
    val lines: List<InvoiceLine> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    val branch: String = "",
)
