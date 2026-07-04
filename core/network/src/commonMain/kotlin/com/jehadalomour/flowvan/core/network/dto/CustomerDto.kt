package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: String,
    val customerNumber: String = "",
    val customerName: String = "",
    val nameAr: String = "",
    val nameEn: String? = null,
    val phone: String? = null,
    val addressAr: String? = null,
    val city: String? = null,
    val cityCode: String? = null,
    val longitude: String? = null,
    val latitude: String? = null,
    val repId: String? = null,
    val regionId: String? = null,
    val category: String? = null,
    val creditLimit: String = "0",
    val paymentTerms: Int = 30,
    val customerType: String = "CASH",        // CASH | CREDIT | WHOLESALE | RETAIL
    val totalDebt: String = "0",
    val totalCredit: String = "0",
    val tin: String? = null,
    val isActive: Boolean = true,
)

@Serializable
data class CreateCustomerRequest(
    val customerNumber: String? = null,   // omit → backend auto-generates (CUST-000001)
    val customerName: String,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val phone: String? = null,
    val addressAr: String? = null,
    val city: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val repId: String? = null,            // assign to the salesman who created it
    val creditLimit: String? = null,
    val customerType: String? = null,
    val regionId: String? = null,
)

@Serializable
data class LogVisitRequest(
    val repId: String,
    val visitedAt: String? = null,
    val hadSale: Boolean = false,
    val visitNote: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)
