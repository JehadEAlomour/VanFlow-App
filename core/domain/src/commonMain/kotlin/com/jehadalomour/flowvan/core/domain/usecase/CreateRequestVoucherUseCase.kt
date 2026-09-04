package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateRequestVoucherUseCase(
    private val invoices: InvoiceRepository,
    private val json: Json,
    private val syncScheduler: SyncScheduler,
    private val voucherNumbers: VoucherNumberGenerator,
    private val location: LocationProvider,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        expectedDeliveryAt: Long?,
        notes: String?,
        /** The customer is tax-exempt — the order carries no tax, like the sale it becomes. */
        taxExempt: Boolean = false,
        taxExemptionNumber: String? = null,
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()

        // A tax-exempt customer's order is stored tax-free (material price + total),
        // so the sales/voucher reports that read this document's stored total show no tax.
        val storedCart = if (taxExempt) InvoiceTaxCalculator.exemptCartLines(cart) else cart

        // lineTaxType already stamped on each CartLine from settings at add-time.
        val summary = InvoiceTaxCalculator.calculateInvoice(
            cart = storedCart,
            invoiceDiscount = InvoiceDiscountInput.None,
        )

        val number = voucherNumbers.next("ORD", "REQUEST")
        val now = Clock.System.now().toEpochMilliseconds()
        val loc = location.lastLocation()
        val invoiceLines = storedCart.map {
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
                unitId      = it.unitId,
                unitConversionQty = it.unitConversionQty,
                taxRate     = it.taxRate,
            )
        }

        val noteParts = listOfNotNull(
            expectedDeliveryAt?.let { "تاريخ التسليم: $it" },
            notes?.takeIf { it.isNotBlank() },
        )

        val entity = InvoiceEntity(
            id             = newVoucherClientRef(),   // unique clientRef; server assigns the real number
            number         = number,
            type           = "REQUEST",
            status         = "CONFIRMED",
            customerId     = customerId,
            salesmanId     = salesmanId,
            createdAt      = now,
            linesJson      = json.encodeToString(invoiceLines),
            isTaxExempt    = taxExempt,
            taxExemptionNumber = if (taxExempt) taxExemptionNumber else null,
            subtotal       = summary.displaySubtotal,
            discountAmount = summary.totalLineDiscounts,
            taxAmount      = summary.totalTax,
            total          = summary.grandTotal,
            paymentMethod  = null,
            notes          = if (noteParts.isEmpty()) null else noteParts.joinToString(" — "),
            syncedAt       = null,
            repLat = loc?.lat,
            repLng = loc?.lng,
        )
        invoices.save(entity)
        // No stock or balance change — intentional for pre-orders.
        syncScheduler.syncNow()
        entity
    }
}
