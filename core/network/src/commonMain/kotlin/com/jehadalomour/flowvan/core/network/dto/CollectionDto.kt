package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollectionDto(
    val id: String,
    val repId: String = "",
    val customerId: String = "",
    val invoiceId: String? = null,
    val paymentId: String? = null,
    val amount: Long = 0,                 // fils
    val method: String = "cash",          // cash | cheque
    val status: String = "pending",       // pending | confirmed | deposited | bounced
    val collectedAt: String? = null,
    val confirmedAt: String? = null,
    val depositedAt: String? = null,
    val note: String? = null,
    val cheque: ChequeDto? = null,
)

@Serializable
data class ChequeDto(
    val id: String = "",
    val bankName: String? = null,
    val chequeNumber: String? = null,
    val payee: String? = null,
    val amount: Long = 0,
    val dueDate: String? = null,
    val status: String = "pending",
)

@Serializable
data class CreateCollectionRequest(
    val repId: String,
    val customerId: String,
    val invoiceId: String? = null,
    val amount: Long,                     // fils, >= 1
    val method: String,                   // cash | cheque
    val collectedAt: String? = null,
    val note: String? = null,
    /** Rep's GPS when recorded — enforces the per-rep location lock. Omitted when null. */
    val repLat: Double? = null,
    val repLng: Double? = null,
    val cheque: CreateChequeRequest? = null,
)

@Serializable
data class CreateChequeRequest(
    val bankName: String? = null,
    val chequeNumber: String? = null,
    val payee: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class CollectionSummaryDto(
    val date: String = "",
    val totalCollectedFils: Long = 0,
    val cashFils: Long = 0,
    val chequeFils: Long = 0,
    val pendingFils: Long = 0,
    val overdueChequeFils: Long = 0,
)
