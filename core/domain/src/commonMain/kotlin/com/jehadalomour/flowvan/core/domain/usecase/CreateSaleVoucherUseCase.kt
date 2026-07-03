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

class StockShortageException(val productId: String, val available: Int, val requested: Int) : Exception(
    "stock shortage for $productId: available=$available requested=$requested",
)

class EmptyCartException : Exception("cart is empty")

class CreateSaleVoucherUseCase(
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

        val number = voucherNumbers.next("INV", "SALE")
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
            id            = number,
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
