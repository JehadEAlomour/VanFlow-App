package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.domain.model.CartLine
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateRequestVoucherUseCase(
    private val invoices: InvoiceRepository,
    private val json: Json,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        expectedDeliveryAt: Long?,
        notes: String?,
    ): Result<InvoiceEntity> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()

        val subtotal = cart.sumOf { it.lineTotal }
        val tax = subtotal * 0.16
        val total = subtotal + tax

        val number = VoucherNumber.next("REQ")
        val now = Clock.System.now().toEpochMilliseconds()
        val invoiceLines = cart.map {
            InvoiceLine(
                productId = it.productId, sku = it.sku, nameAr = it.nameAr,
                qty = it.qty, unitPrice = it.unitPrice, discountPct = it.discountPct,
                lineTotal = it.lineTotal,
            )
        }

        val noteParts = listOfNotNull(
            expectedDeliveryAt?.let { "تاريخ التسليم: $it" },
            notes?.takeIf { it.isNotBlank() },
        )
        val noteText = if (noteParts.isEmpty()) null else noteParts.joinToString(" — ")

        val entity = InvoiceEntity(
            id = "REQ-$number",
            number = number,
            type = "REQUEST",
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
            notes = noteText,
            syncedAt = null,
        )
        invoices.save(entity)
        // No stock or balance change — intentional.
        entity
    }
}
