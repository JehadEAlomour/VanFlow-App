package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
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

class StockShortageException(val productId: String, val available: Int, val requested: Int) : Exception(
    "stock shortage for $productId: available=$available requested=$requested",
)

class EmptyCartException : Exception("cart is empty")

/**
 * A credit (on-account) sale would push the customer over their credit limit. Hard block —
 * the remedy is a manager raising the limit (mirrors down on the next sync), then the rep
 * re-creates the voucher. See docs/SPEC-accounts-receivable.md.
 */
class CreditLimitExceededException(
    val creditLimit: Double,
    val balance: Double,
    val attempted: Double,
) : Exception("credit limit exceeded: limit=$creditLimit balance=$balance attempted=$attempted") {
    /** Remaining headroom before the sale (never negative). */
    val available: Double get() = (creditLimit - balance).coerceAtLeast(0.0)
}

/**
 * The customer has no credit limit set (limit ≤ 0), so credit (on-account) sales are not
 * allowed at all. The rep must sell for cash, or a manager must set a limit first.
 * See docs/SPEC-accounts-receivable.md.
 */
class NoCreditLimitException(val customerName: String?) :
    Exception("customer has no credit limit set")

class CreateSaleVoucherUseCase(
    private val invoices: InvoiceRepository,
    private val products: ProductRepository,
    private val productUnits: ProductUnitRepository,
    private val customers: CustomerRepository,
    private val json: Json,
    private val syncScheduler: SyncScheduler,
    private val voucherNumbers: VoucherNumberGenerator,
    private val location: LocationProvider,
    private val locationGate: LocationGate,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        discountAmount: Double,
        paymentMethod: PaymentMethod,
        notes: String?,
        chosenFreeItems: List<String> = emptyList(),
        /**
         * The free (gift) lines the offers engine granted for this cart. They cost nothing
         * but they leave the van like any other piece, so they are checked and deducted
         * alongside [cart] — see [saleStockDemand]. Empty → no gifts.
         */
        freeLines: List<FreeLine> = emptyList(),
        /**
         * The cart with the offers engine's per-line discounts overlaid (the same lines
         * the cart screen shows). When offers applied this differs from [cart]; we store
         * the OFFER-APPLIED result as the invoice's primary/display totals so the saved
         * and printed invoice matches the cart even offline. Null / equal to [cart] → no
         * offers, behaviour unchanged. The RAW [cart] is still what uploads — the server
         * re-applies offers, so uploading the offer-applied cart would double-discount.
         */
        offerAdjustedCart: List<CartLine>? = null,
        /**
         * Offers applied at sale time (name + discount value in JOD), frozen for the printed
         * receipt so the footer can itemize each offer plus a total. Empty → no offers; the
         * footer falls back to the generic discount row.
         */
        appliedOffers: List<InvoiceAppliedOffer> = emptyList(),
    ): Result<InvoiceEntity> = runCatching {
        // A location-locked rep writes nothing while the phone denies location.
        // Before the empty-cart check so the reason given is the real one.
        locationGate.require()
        if (cart.isEmpty()) throw EmptyCartException()

        // Check the SAME pools, in the SAME scale, that the decrement below will move —
        // the sold lines AND the offers' gift lines, each pool tallied once. This used to
        // compare line.qty (a count of units) against vanStock (base pieces), so 5 cartons
        // of 12 were checked against 5 and then deducted as 60; and it ignored gifts, so
        // "buy 6 get 1 free" off the last 6 pieces passed here and failed on sync.
        val demand = saleStockDemand(products, productUnits, cart, freeLines)
        for (pool in demand) {
            if (pool.requestedBase > pool.available) {
                throw StockShortageException(pool.productId, pool.available, pool.requestedBase)
            }
        }

        val invoiceDiscount = if (discountAmount > 0.0)
            InvoiceDiscountInput.Fixed(discountAmount) else InvoiceDiscountInput.None

        // Is this an exempt sale? Same rule the server applies — the flag AND the
        // validity window — so the totals saved here match the voucher it posts.
        // Resolved once and frozen onto the invoice: a receipt reprinted after the
        // certificate lapses must still show the exemption it was issued under.
        val saleCustomer = customers.findById(customerId)
        val taxExempt = saleCustomer?.let { c ->
            val now = Clock.System.now().toEpochMilliseconds()
            c.isTaxExempt &&
                (c.taxExemptionValidFrom == null || now >= c.taxExemptionValidFrom!!) &&
                (c.taxExemptionValidTo == null || now <= c.taxExemptionValidTo!!)
        } ?: false

        // Use the full tax calculator — lineTaxType is already stamped on each CartLine
        // by the ViewModel based on the active AppSettings.
        val rawSummary = InvoiceTaxCalculator.calculateInvoice(cart, invoiceDiscount, taxExempt)

        // Offers were applied when a distinct offer-adjusted cart was supplied. The
        // DISPLAY cart drives the primary/stored totals; the RAW cart drives the upload.
        val offersApplied = offerAdjustedCart != null && offerAdjustedCart != cart
        val displayCart = if (offersApplied) offerAdjustedCart!! else cart
        val displaySummary =
            if (offersApplied) InvoiceTaxCalculator.calculateInvoice(displayCart, invoiceDiscount, taxExempt)
            else rawSummary

        // AR credit-limit guard (runs BEFORE any stock/balance mutation):
        //  • No limit set (≤ 0)                → block: this customer can't buy on credit.
        //  • balance + sale > limit            → block: over the limit.
        // The local balance already reflects prior (even unsynced) credit sales, so this
        // is offline-safe. See docs/SPEC-accounts-receivable.md.
        if (paymentMethod == PaymentMethod.CREDIT) {
            val customer = saleCustomer
            if (customer != null) {
                if (customer.creditLimit <= 0.0) {
                    throw NoCreditLimitException(customer.nameAr)
                }
                if (customer.balance + displaySummary.grandTotal > customer.creditLimit + 0.0001) {
                    throw CreditLimitExceededException(
                        creditLimit = customer.creditLimit,
                        balance = customer.balance,
                        attempted = displaySummary.grandTotal,
                    )
                }
            }
        }

        val number = voucherNumbers.next("INV", "SALE")
        val now = Clock.System.now().toEpochMilliseconds()
        // Capture the rep's position at sale time for the location lock. Persisted on
        // the entity so it survives an offline delay and reaches the backend on sync.
        val loc = location.lastLocation()

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

        // The lines we STORE for local display + PRINT. For a tax-exempt customer the
        // material price must print without tax and the lines must sum to the tax-free
        // total, so re-price them the same way the exempt summary does (strip inclusive
        // tax, zero the rate). The server upload below keeps the un-stripped lines, so
        // the backend still applies the exemption exactly once — sync is unchanged.
        val storedCart = if (taxExempt) InvoiceTaxCalculator.exemptCartLines(displayCart) else displayCart

        val entity = InvoiceEntity(
            id            = newVoucherClientRef(),   // unique clientRef; server assigns the real number
            number        = number,
            type          = "SALE",
            status        = "CONFIRMED",
            customerId    = customerId,
            salesmanId    = salesmanId,
            createdAt     = now,
            // Primary/display fields carry the OFFER-APPLIED result (matches the cart),
            // tax-stripped when the customer is exempt so the print reads tax-free.
            linesJson     = json.encodeToString(storedCart.toInvoiceLines()),
            isTaxExempt   = taxExempt,
            taxExemptionNumber = if (taxExempt) saleCustomer?.taxExemptionNumber else null,
            subtotal      = displaySummary.displaySubtotal,
            discountAmount = displaySummary.totalLineDiscounts + displaySummary.invoiceDiscountAmount,
            taxAmount     = displaySummary.totalTax,
            total         = displaySummary.grandTotal,
            paymentMethod = paymentMethod.name,
            notes         = notes,
            syncedAt      = null,
            chosenFreeItemsCsv = chosenFreeItems
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(","),
            repLat = loc?.lat,
            repLng = loc?.lng,
            // Upload snapshot: the un-stripped lines so the backend applies offers AND the
            // tax exemption exactly once. With offers → the RAW pre-offer cart. Exempt (no
            // offers) → the display cart un-stripped, so the server receives the same data
            // it did before the print-side stripping above (sync behaviour is unchanged).
            uploadLinesJson = when {
                offersApplied -> json.encodeToString(cart.toInvoiceLines())
                taxExempt     -> json.encodeToString(displayCart.toInvoiceLines())
                else          -> null
            },
            uploadDiscountAmount =
                if (offersApplied) rawSummary.totalLineDiscounts + rawSummary.invoiceDiscountAmount else null,
            // Frozen per-offer breakdown for the printed footer (null when no offers applied).
            appliedOffersJson = appliedOffers
                .takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) },
            // The gift lines themselves. appliedOffersJson carries each offer's NAME and
            // AMOUNT but no item numbers, and chosenFreeItemsCsv carries the rep's picks
            // rather than what the evaluation actually produced — so without this nothing
            // local knows which pieces left the van for free, and a return built from this
            // sale silently drops them.
            freeLinesJson = freeLines
                .takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) },
        )

        invoices.save(entity)
        // Local van stock only — the server derives stock from the posted voucher transaction,
        // gift lines included, so the gifts are deducted here too or the van's count drifts
        // one piece per gift from the server's.
        for (pool in demand) {
            applyVanStockDelta(products, productUnits, pool, -pool.requestedBase)
        }
        if (paymentMethod == PaymentMethod.CREDIT) {
            // The customer owes the offer-applied total, not the pre-offer amount.
            customers.adjustBalance(customerId, displaySummary.grandTotal)
        }
        syncScheduler.syncNow()   // push invoice to backend now; stays flagged (synced=null) if offline
        entity
    }
}
