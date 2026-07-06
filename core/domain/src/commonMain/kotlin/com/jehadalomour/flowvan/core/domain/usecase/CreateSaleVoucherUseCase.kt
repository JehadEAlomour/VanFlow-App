package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.data.location.LocationProvider
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
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()

        for (line in cart) {
            val product = products.findById(line.productId)
                ?: error("product ${line.productId} not found")
            if (line.qty.toInt() > product.vanStock) {
                throw StockShortageException(line.productId, product.vanStock, line.qty.toInt())
            }
        }

        // Use the full tax calculator — lineTaxType is already stamped on each CartLine
        // by the ViewModel based on the active AppSettings.
        val summary = InvoiceTaxCalculator.calculateInvoice(
            cart = cart,
            invoiceDiscount = if (discountAmount > 0.0)
                InvoiceDiscountInput.Fixed(discountAmount) else InvoiceDiscountInput.None,
        )

        // AR credit-limit guard: a credit (on-account) sale may not push the customer's
        // balance over their limit. The local balance already reflects prior (even
        // unsynced) credit sales, so this is offline-safe. A limit of 0 = not enforced.
        // Runs BEFORE any stock/balance mutation. See docs/SPEC-accounts-receivable.md.
        if (paymentMethod == PaymentMethod.CREDIT) {
            val customer = customers.findById(customerId)
            if (customer != null && customer.creditLimit > 0.0 &&
                customer.balance + summary.grandTotal > customer.creditLimit + 0.0001
            ) {
                throw CreditLimitExceededException(
                    creditLimit = customer.creditLimit,
                    balance = customer.balance,
                    attempted = summary.grandTotal,
                )
            }
        }

        val number = voucherNumbers.next("INV", "SALE")
        val now = Clock.System.now().toEpochMilliseconds()
        // Capture the rep's position at sale time for the location lock. Persisted on
        // the entity so it survives an offline delay and reaches the backend on sync.
        val loc = location.lastLocation()
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
            id            = newVoucherClientRef(),   // unique clientRef; server assigns the real number
            number        = number,
            type          = "SALE",
            status        = "CONFIRMED",
            customerId    = customerId,
            salesmanId    = salesmanId,
            createdAt     = now,
            linesJson     = json.encodeToString(invoiceLines),
            subtotal      = summary.subtotalBeforeDiscounts,
            discountAmount = summary.totalLineDiscounts + summary.invoiceDiscountAmount,
            taxAmount     = summary.totalTax,
            total         = summary.grandTotal,
            paymentMethod = paymentMethod.name,
            notes         = notes,
            syncedAt      = null,
            chosenFreeItemsCsv = chosenFreeItems
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(","),
            repLat = loc?.lat,
            repLng = loc?.lng,
        )

        invoices.save(entity)
        // Local van stock only — the server derives stock from the posted voucher transaction.
        for (line in cart) {
            products.adjustStock(line.productId, -line.stockQty.toInt())
        }
        if (paymentMethod == PaymentMethod.CREDIT) {
            customers.adjustBalance(customerId, summary.grandTotal)
        }
        syncScheduler.syncNow()   // push invoice to backend now; stays flagged (synced=null) if offline
        entity
    }
}
