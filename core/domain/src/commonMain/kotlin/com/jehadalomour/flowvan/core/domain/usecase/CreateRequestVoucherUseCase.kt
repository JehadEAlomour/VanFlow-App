package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.InvoiceAppliedOffer
import com.jehadalomour.flowvan.core.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.PaymentMethod
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
        // ── Offers ────────────────────────────────────────────────────────────
        // An order is quoted at the price the customer will actually be invoiced,
        // so the same offers a sale would get are applied here. Mirrors the sale's
        // shape exactly: [cart] is the RAW pre-offer cart and is what uploads (the
        // server re-evaluates and would otherwise double-discount), while
        // [offerAdjustedCart] drives the stored and displayed totals.
        /** The rep's GIFT picks — the server turns them into free lines on upload. */
        chosenFreeItems: List<String> = emptyList(),
        /** Gift lines the offers produced. NO van stock moves: an order ships nothing. */
        freeLines: List<FreeLine> = emptyList(),
        /** Offer-applied lines. Null (or equal to [cart]) means no offer applied. */
        offerAdjustedCart: List<CartLine>? = null,
        /** Per-offer breakdown, frozen for the printed footer. */
        appliedOffers: List<InvoiceAppliedOffer> = emptyList(),
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()

        // Offers applied when a DISTINCT offer-adjusted cart came in. The display
        // cart drives what is stored and printed; the raw cart is what uploads.
        val offersApplied = offerAdjustedCart != null && offerAdjustedCart != cart
        val displayCart = if (offersApplied) offerAdjustedCart!! else cart

        // A tax-exempt customer's order is stored tax-free (material price + total),
        // so the sales/voucher reports that read this document's stored total show no tax.
        val storedCart = if (taxExempt) InvoiceTaxCalculator.exemptCartLines(displayCart) else displayCart

        // lineTaxType already stamped on each CartLine from settings at add-time.
        val summary = InvoiceTaxCalculator.calculateInvoice(
            cart = storedCart,
            invoiceDiscount = InvoiceDiscountInput.None,
        )
        // The pre-offer figures, for the upload snapshot below.
        val rawSummary =
            if (offersApplied) {
                InvoiceTaxCalculator.calculateInvoice(
                    cart = if (taxExempt) InvoiceTaxCalculator.exemptCartLines(cart) else cart,
                    invoiceDiscount = InvoiceDiscountInput.None,
                )
            } else {
                summary
            }

        val number = voucherNumbers.next("ORD", "REQUEST")
        val now = Clock.System.now().toEpochMilliseconds()
        val loc = location.lastLocation()
        // One mapping, used for both the stored lines and the raw upload snapshot.
        fun List<CartLine>.toInvoiceLines(): List<InvoiceLine> = map {
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
            linesJson      = json.encodeToString(storedCart.toInvoiceLines()),
            isTaxExempt    = taxExempt,
            taxExemptionNumber = if (taxExempt) taxExemptionNumber else null,
            subtotal       = summary.displaySubtotal,
            discountAmount = summary.totalLineDiscounts,
            taxAmount      = summary.totalTax,
            total          = summary.grandTotal,
            // An order is taken on account: nobody pays for goods that have not
            // been delivered, so CREDIT is what it is. It used to be stored null
            // and every reader inferred credit from the absence — the cash-flow
            // report by convention, the printed slip by leaving the line blank.
            // Recording it says the same thing the server now records, so the
            // offline copy and the synced one agree instead of coinciding.
            paymentMethod  = PaymentMethod.CREDIT.name,
            notes          = if (noteParts.isEmpty()) null else noteParts.joinToString(" — "),
            syncedAt       = null,
            repLat = loc?.lat,
            repLng = loc?.lng,
            // The rep's GIFT picks travel to the server, which turns them into the
            // free lines — the same route a sale's picks take.
            chosenFreeItemsCsv = chosenFreeItems
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(","),
            // Upload snapshot: the RAW pre-offer lines. The server re-evaluates
            // offers on ingest, so uploading the already-discounted cart would
            // discount it twice. Null when no offer applied — then the primary
            // lines above are already the raw ones.
            uploadLinesJson =
                if (offersApplied) json.encodeToString(cart.toInvoiceLines()) else null,
            uploadDiscountAmount =
                if (offersApplied) rawSummary.totalLineDiscounts else null,
            // Frozen per-offer breakdown for the printed footer.
            appliedOffersJson = appliedOffers
                .takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) },
        )
        invoices.save(entity)
        // No stock or balance change — intentional for pre-orders. That holds for
        // the gift lines too: an order delivers nothing, so nothing leaves the van
        // yet. The sale this becomes is what moves the stock.
        syncScheduler.syncNow()
        entity
    }
}
