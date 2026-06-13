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
)