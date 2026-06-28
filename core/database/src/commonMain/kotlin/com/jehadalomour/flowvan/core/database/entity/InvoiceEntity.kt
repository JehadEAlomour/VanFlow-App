package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val number: String,
    val type: String,
    val status: String,
    val customerId: String,
    val salesmanId: String,
    val createdAt: Long,
    val linesJson: String,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: String?,
    val notes: String?,
    val syncedAt: Long?,
    /** For RETURN vouchers: the original SALE invoice this return is issued against. */
    val referenceInvoiceId: String? = null,
    val referenceNumber: String? = null,
    /**
     * GIFT picks for ITEM_QTY_REWARD offers, as a comma-joined list of item numbers
     * (e.g. "ITM-1,ITM-3"). Null/blank when the sale carries no gift picks. Sent to the
     * server on sync so it adds the free lines and records the redemption.
     */
    val chosenFreeItemsCsv: String? = null,
)