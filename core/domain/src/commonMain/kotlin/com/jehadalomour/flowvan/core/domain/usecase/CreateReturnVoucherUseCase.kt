package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateReturnVoucherUseCase(
    private val invoices: InvoiceRepository,
    private val products: ProductRepository,
    private val customers: CustomerRepository,
    private val json: Json,
    private val syncScheduler: SyncScheduler,
    private val voucherNumbers: VoucherNumberGenerator,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        reason: String,
        paymentMethod: PaymentMethod,
        extraNotes: String?,
        referenceInvoiceId: String? = null,
        referenceNumber: String? = null,
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()
        require(reason.isNotBlank()) { "reason required" }

        // lineTaxType already stamped on each CartLine from settings at add-time.
        val summary = InvoiceTaxCalculator.calculateInvoice(
            cart = cart,
            invoiceDiscount = InvoiceDiscountInput.None,
        )

        val number = voucherNumbers.next("RET", "RETURN")
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
                unitConversionQty = it.unitConversionQty,
                taxRate     = it.taxRate,
            )
        }

        val entity = InvoiceEntity(
            id             = newVoucherClientRef(),   // unique clientRef; server assigns the real number
            number         = number,
            type           = "RETURN",
            status         = "CONFIRMED",
            customerId     = customerId,
            salesmanId     = salesmanId,
            createdAt      = now,
            linesJson      = json.encodeToString(invoiceLines),
            subtotal       = summary.subtotalBeforeDiscounts,
            discountAmount = summary.totalLineDiscounts,
            taxAmount      = summary.totalTax,
            total          = summary.grandTotal,
            paymentMethod  = paymentMethod.name,
            notes          = "سبب: $reason${extraNotes?.let { " — $it" } ?: ""}",
            syncedAt       = null,
            referenceInvoiceId = referenceInvoiceId,
            referenceNumber    = referenceNumber,
        )

        invoices.save(entity)
        // Local van stock only — the server derives stock from the posted voucher transaction.
        for (line in cart) {
            products.adjustStock(line.productId, line.stockQty.toInt())
        }
        // A CREDIT return is a credit note → reduces the customer's balance (debt).
        // A CASH return refunds cash from the drawer → no balance change.
        if (paymentMethod == PaymentMethod.CREDIT) {
            customers.adjustBalance(customerId, -summary.grandTotal)
        }
        syncScheduler.syncNow()
        entity
    }
}
