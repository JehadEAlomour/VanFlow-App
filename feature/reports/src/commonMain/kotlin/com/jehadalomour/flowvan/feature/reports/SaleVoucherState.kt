package com.jehadalomour.flowvan.feature.reports

import com.jehadalomour.flowvan.core.domain.AppError
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Customer
import com.jehadalomour.flowvan.core.model.InvoiceTotals
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.model.Product

data class SaleVoucherState(
    val customer: Customer? = null,
    val allProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val overallDiscount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val totals: InvoiceTotals = InvoiceTotals.EMPTY,
    val cartVisible: Boolean = false,
    val paymentSheetVisible: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null
)