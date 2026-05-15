package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
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

        val subtotal = cart.sumOf { it.lineTotal }
        val taxBase = (subtotal - discountAmount).coerceAtLeast(0.0)
        val tax = taxBase * 0.16
        val total = taxBase + tax

        val number = VoucherNumber.next("INV")
        val now = Clock.System.now().toEpochMilliseconds()
        val invoiceLines = cart.map {
            InvoiceLine(
                productId = it.productId,
                sku = it.sku,
                nameAr = it.nameAr,
                qty = it.qty,
                unitPrice = it.unitPrice,
                discountPct = it.discountPct,
                lineTotal = it.lineTotal,
            )
        }

        val entity = InvoiceEntity(
            id = "INV-$number",
            number = number,
            type = "SALE",
            status = "CONFIRMED",
            customerId = customerId,
            salesmanId = salesmanId,
            createdAt = now,
            linesJson = json.encodeToString(invoiceLines),
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = tax,
            total = total,
            paymentMethod = paymentMethod.name,
            notes = notes,
            syncedAt = null,
        )

        invoices.save(entity)
        for (line in cart) {
            products.adjustStock(line.productId, -line.qty.toInt())
        }
        if (paymentMethod == PaymentMethod.CREDIT) {
            customers.adjustBalance(customerId, total)
        }
        entity
    }
}
