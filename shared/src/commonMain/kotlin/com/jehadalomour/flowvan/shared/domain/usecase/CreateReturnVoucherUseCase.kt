package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateReturnVoucherUseCase(
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
        reason: String,
        extraNotes: String?,
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()
        require(reason.isNotBlank()) { "reason required" }

        val subtotal = cart.sumOf { it.lineTotal }
        val tax = subtotal * 0.16
        val total = subtotal + tax

        val number = VoucherNumber.next("RET")
        val now = Clock.System.now().toEpochMilliseconds()
        val invoiceLines = cart.map {
            InvoiceLine(
                productId = it.productId, sku = it.sku, nameAr = it.nameAr,
                qty = it.qty, unitPrice = it.unitPrice, discountPct = it.discountPct,
                lineTotal = it.lineTotal,
            )
        }

        val entity = InvoiceEntity(
            id = "RET-$number",
            number = number,
            type = "RETURN",
            status = "CONFIRMED",
            customerId = customerId,
            salesmanId = salesmanId,
            createdAt = now,
            linesJson = json.encodeToString(invoiceLines),
            subtotal = subtotal,
            discountAmount = 0.0,
            taxAmount = tax,
            total = total,
            paymentMethod = null,
            notes = "سبب: $reason${extraNotes?.let { " — $it" } ?: ""}",
            syncedAt = null,
        )

        invoices.save(entity)
        for (line in cart) {
            products.adjustStock(line.productId, line.qty.toInt())
        }
        customers.adjustBalance(customerId, -total)
        entity
    }
}
