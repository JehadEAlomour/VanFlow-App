package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.network.api.VoucherApi
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A line of an original SALE, used to pre-fill a return (percentages as fractions). */
data class SourceSaleLine(
    val sku: String,
    val name: String,
    val qty: Double,
    val unitPrice: Double,
    val taxRate: Double,
    val discountPct: Double,
    val unitName: String?,
    val unitBaseQty: Int?,
)

/**
 * Fetches a customer's SALE vouchers from the server so a RETURN can reference the
 * authoritative server voucher number (the app never stores it locally). Returns are
 * built from the chosen sale's own lines, so the backend's item/store checks pass.
 */
class GetCustomerSalesUseCase(
    private val voucherApi: VoucherApi,
) {
    /** Look up a single SALE by its voucher number for a customer (manual return source). */
    suspend fun byNumber(voucherNumber: String, customerNumber: String): InvoiceEntity? =
        voucherApi.saleByNumber(voucherNumber, customerNumber)
            ?.takeIf { it.isPosted }
            ?.let { dto ->
                InvoiceEntity(
                    id = dto.id,
                    number = dto.voucherNumber,
                    type = "SALE",
                    status = "CONFIRMED",
                    customerId = "",
                    salesmanId = "",
                    createdAt = parseMs(dto.createdAt ?: dto.inDate),
                    linesJson = "[]",
                    subtotal = 0.0,
                    discountAmount = 0.0,
                    taxAmount = 0.0,
                    total = dto.netTotal.toDoubleOrNull() ?: 0.0,
                    paymentMethod = null,
                    notes = null,
                    syncedAt = null,
                )
            }

    /** The chosen sale's lines, to pre-fill the return cart. */
    suspend fun lines(saleId: String): List<SourceSaleLine> =
        voucherApi.voucherDetail(saleId).transactions.map { t ->
            SourceSaleLine(
                sku = t.itemNumber,
                name = t.itemName,
                qty = t.itemQty.toDoubleOrNull() ?: 0.0,
                unitPrice = t.unitPrice.toDoubleOrNull() ?: 0.0,
                taxRate = (t.taxPercentage.toDoubleOrNull() ?: 0.0) / 100.0,
                discountPct = (t.discountPercentage.toDoubleOrNull() ?: 0.0) / 100.0,
                unitName = t.unitName,
                unitBaseQty = t.unitBaseQty,
            )
        }

    @OptIn(ExperimentalTime::class)
    private fun parseMs(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(s).toEpochMilliseconds() }
            .getOrElse {
                runCatching { Instant.parse("${s}T00:00:00Z").toEpochMilliseconds() }
                    .getOrDefault(0L)
            }
    }
}
