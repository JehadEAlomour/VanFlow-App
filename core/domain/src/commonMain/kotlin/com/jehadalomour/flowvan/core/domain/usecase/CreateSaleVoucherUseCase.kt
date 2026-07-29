package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.model.CartLine
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
    private val customers: CustomerRepository,
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
        discountAmount: Double,
        paymentMethod: PaymentMethod,
        notes: String?,
        chosenFreeItems: List<String> = emptyList(),
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
        if (cart.isEmpty()) throw EmptyCartException()

        for (line in cart) {
            val product = products.findById(line.productId)
                ?: error("product ${line.productId} not found")
            if (line.qty.toInt() > product.vanStock) {
                throw StockShortageException(line.productId, product.vanStock, line.qty.toInt())
            }
        }

        val invoiceDiscount = if (discountAmount > 0.0)
            InvoiceDiscountInput.Fixed(discountAmount) else InvoiceDiscountInput.None
        // Use the full tax calculator — lineTaxType is already stamped on each CartLine
        // by the ViewModel based on the active AppSettings.
        val rawSummary = InvoiceTaxCalculator.calculateInvoice(cart, invoiceDiscount)

        // Offers were applied when a distinct offer-adjusted cart was supplied. The
        // DISPLAY cart drives the primary/stored totals; the RAW cart drives the upload.
        val offersApplied = offerAdjustedCart != null && offerAdjustedCart != cart
        val displayCart = if (offersApplied) offerAdjustedCart!! else cart
        val displaySummary =
            if (offersApplied) InvoiceTaxCalculator.calculateInvoice(displayCart, invoiceDiscount)
            else rawSummary

        // AR credit-limit guard (runs BEFORE any stock/balance mutation):
        //  • No limit set (≤ 0)                → block: this customer can't buy on credit.
        //  • balance + sale > limit            → block: over the limit.
        // The local balance already reflects prior (even unsynced) credit sales, so this
        // is offline-safe. See docs/SPEC-accounts-receivable.md.
        if (paymentMethod == PaymentMethod.CREDIT) {
            val customer = customers.findById(customerId)
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
                unitConversionQty = it.unitConversionQty,
                taxRate     = it.taxRate,
            )
        }

        val entity = InvoiceEntity(
            id            = newVoucherClientRef(),   // unique clientRef; server assigns the real number
            number        = number,
            type          = "SALE",
            status        = "CONFIRMED",
            customerId    = customerId,
            salesmanId    = salesmanId,
            createdAt     = now,
            // Primary/display fields carry the OFFER-APPLIED result (matches the cart).
            linesJson     = json.encodeToString(displayCart.toInvoiceLines()),
            subtotal      = displaySummary.subtotalBeforeDiscounts,
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
            // Upload snapshot: the RAW cart (manual discounts only) so the backend applies
            // offers exactly once. Null when no offers → upload uses the primary fields.
            uploadLinesJson =
                if (offersApplied) json.encodeToString(cart.toInvoiceLines()) else null,
            uploadDiscountAmount =
                if (offersApplied) rawSummary.totalLineDiscounts + rawSummary.invoiceDiscountAmount else null,
            // Frozen per-offer breakdown for the printed footer (null when no offers applied).
            appliedOffersJson = appliedOffers
                .takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) },
        )

        invoices.save(entity)
        // Local van stock only — the server derives stock from the posted voucher transaction.
        for (line in cart) {
            products.adjustStock(line.productId, -line.stockQty.toInt())
        }
        if (paymentMethod == PaymentMethod.CREDIT) {
            // The customer owes the offer-applied total, not the pre-offer amount.
            customers.adjustBalance(customerId, displaySummary.grandTotal)
        }
        syncScheduler.syncNow()   // push invoice to backend now; stays flagged (synced=null) if offline
        entity
    }
}
