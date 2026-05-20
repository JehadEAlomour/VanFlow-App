package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceTaxCalculator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateRequestVoucherUseCase(
    private val invoices: InvoiceRepository,
    private val json: Json,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        expectedDeliveryAt: Long?,
        notes: String?,
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()

        // lineTaxType already stamped on each CartLine from settings at add-time.
        val summary = InvoiceTaxCalculator.calculateInvoice(
            cart = cart,
            invoiceDiscount = InvoiceDiscountInput.None,
        )

        val number = VoucherNumber.next("REQ")
        val now = Clock.System.now().toEpochMilliseconds()
        val invoiceLines = cart.map {
            InvoiceLine(
                productId   = it.productId,
                sku         = it.sku,
                nameAr      = it.nameAr,
                qty         = it.qty,
                unitPrice   = it.unitPrice,
                discountPct = it.discountPct,
                lineTotal   = it.lineTotal,
                taxType     = it.lineTaxType.name,
                taxAmount   = it.lineTax,
                unit        = it.unit,
                taxRate     = it.taxRate,
            )
        }

        val noteParts = listOfNotNull(
            expectedDeliveryAt?.let { "تاريخ التسليم: $it" },
            notes?.takeIf { it.isNotBlank() },
        )

        val entity = InvoiceEntity(
            id             = "REQ-$number",
            number         = number,
            type           = "REQUEST",
            status         = "CONFIRMED",
            customerId     = customerId,
            salesmanId     = salesmanId,
            createdAt      = now,
            linesJson      = json.encodeToString(invoiceLines),
            subtotal       = summary.subtotalBeforeDiscounts,
            discountAmount = summary.totalLineDiscounts,
            taxAmount      = summary.totalTax,
            total          = summary.grandTotal,
            paymentMethod  = null,
            notes          = if (noteParts.isEmpty()) null else noteParts.joinToString(" — "),
            syncedAt       = null,
        )
        invoices.save(entity)
        // No stock or balance change — intentional for pre-orders.
        entity
    }
}
