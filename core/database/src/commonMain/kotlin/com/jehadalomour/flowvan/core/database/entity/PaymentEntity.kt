package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val number: String,
    val customerId: String,
    val salesmanId: String,
    val amount: Double,
    val method: String,
    val status: String,
    val createdAt: Long,
    val chequeNumber: String?,
    val chequeBank: String?,
    val chequeDate: Long?,
    val transferRef: String?,
    val notes: String?,
    val syncedAt: Long?,
)