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
    /** Assigned price list id (price_lists.id). Null = base catalog prices. */
    val priceListId: String? = null,
    // ── Tax exemption ────────────────────────────────────────────────────────
    // Every voucher for this customer is issued tax-exempt. Nullable, not
    // `Boolean = false`: a default only covers a MISSING key, and an explicit
    // null on the wire would throw.
    val isTaxExempt: Boolean? = null,
    val taxExemptionType: String? = null,
    val taxExemptionNumber: String? = null,
    val taxExemptionReason: String? = null,
    /** Outside this window the customer is NOT exempt, flag or no flag. */
    val taxExemptionValidFrom: String? = null,
    val taxExemptionValidTo: String? = null,
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
    /**
     * Id from POST /customers/photo. REQUIRED when a salesman creates the
     * customer — the backend refuses without it (400 "A customer document photo
     * is required..."), because the photo is staged before the customer exists.
     */
    val photoId: String? = null,
    val creditLimit: String? = null,
    val customerType: String? = null,
    val regionId: String? = null,
    // A customer created in the field can be exempt from the moment they exist,
    // rather than only after the next ERP sync catches up.
    val isTaxExempt: Boolean? = null,
    val taxExemptionType: String? = null,
    val taxExemptionNumber: String? = null,
    val taxExemptionReason: String? = null,
    /**
     * The prospecting lead this customer was filed from, when it came through
     * Find Customers. The backend stamps source='PROSPECTING' and closes the
     * lead; omit for a hand-typed customer.
     */
    val sourceProspectId: String? = null,
)

/** Body for `POST /customers/:id/location` — a rep seeding a missing store pin. */
@Serializable
data class SeedLocationRequest(
    val lat: Double,
    val lng: Double,
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

/**
 * A customer document photo uploaded BEFORE the customer exists. [id] is what
 * travels back on CreateCustomerRequest.photoId.
 */
@Serializable
data class StagedPhotoDto(
    val id: String,
    val url: String = "",
    val originalName: String = "",
    val mimeType: String = "",
    val sizeBytes: Int = 0,
)
