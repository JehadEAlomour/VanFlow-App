package com.jehadalomour.flowvan.shared.domain.model

enum class PaymentMethod { CASH, CHEQUE, TRANSFER, CREDIT }

enum class PaymentStatus { PENDING, CONFIRMED, BOUNCED }

data class Payment(
    val id: String,
    val number: String,
    val customerId: String,
    val salesmanId: String,
    val amount: Double,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val createdAt: Long,
    val chequeNumber: String?,
    val chequeBank: String?,
    val chequeDate: Long?,
    val transferRef: String?,
    val notes: String?,
)