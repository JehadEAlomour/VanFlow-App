package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.shared.domain.model.PaymentMethod
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
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        discountAmount: Double,
        paymentMethod: PaymentMethod,
        notes: String?,
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

        val number = VoucherNumber.next("INV")
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

        val entity = InvoiceEntity(
            id            = "INV-$number",
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
        )

        invoices.save(entity)
        for (line in cart) {
            products.adjustStock(line.productId, -line.stockQty.toInt())
        }
        if (paymentMethod == PaymentMethod.CREDIT) {
            customers.adjustBalance(customerId, summary.grandTotal)
        }
        entity
    }
}
