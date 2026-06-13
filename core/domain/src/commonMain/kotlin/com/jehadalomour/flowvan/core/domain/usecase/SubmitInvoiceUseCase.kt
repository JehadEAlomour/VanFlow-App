package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.network.api.InvoiceApi
import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceLine
import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceRequest
import com.jehadalomour.flowvan.core.network.mapper.toDomain
import com.jehadalomour.flowvan.core.network.http.NetworkException
import com.jehadalomour.flowvan.core.network.http.jodToFils
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.Invoice
import com.jehadalomour.flowvan.core.model.PaymentMethod
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Creates a draft invoice on the backend then confirms it (submits to JoFotara).
 * Returns the confirmed [Invoice]. Local Room caching is handled by the sync engine.
 */
class SubmitInvoiceUseCase(
    private val invoiceApi: InvoiceApi,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        repId: String,
        lines: List<CartLine>,
        invoiceDiscountValue: Double = 0.0,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        note: String? = null,
    ): Result<Invoice> {
        if (lines.isEmpty()) return Result.failure(IllegalArgumentException("empty cart"))
        return try {
            val request = CreateInvoiceRequest(
                customerId = customerId,
                repId = repId,
                lines = lines.map { line ->
                    CreateInvoiceLine(
                        productId = line.productId,
                        quantity = line.qty,
                        unitPrice = line.unitPrice.jodToFils().toLong(),
                        lineDiscountType = "PERCENTAGE",
                        lineDiscountValue = line.discountPct * 100.0,
                    )
                },
                invoiceDiscountType = if (invoiceDiscountValue > 0) "FIXED_AMOUNT" else null,
                invoiceDiscountValue = if (invoiceDiscountValue > 0) invoiceDiscountValue.jodToFils().toDouble() else null,
                paymentMethodCode = if (paymentMethod == PaymentMethod.CREDIT) "022" else "012",
                note = note,
            )
            val draft = invoiceApi.create(request)
            val confirmed = invoiceApi.confirm(draft.id)
            val now = Clock.System.now().toEpochMilliseconds()
            Result.success(confirmed.toDomain(now))
        } catch (e: NetworkException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
