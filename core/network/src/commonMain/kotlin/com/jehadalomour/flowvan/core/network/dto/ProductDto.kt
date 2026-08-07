package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val itemNumber: String = "",
    val sku: String = "",
    val barcode: String = "",
    val name: String = "",
    val nameAr: String = "",
    val nameEn: String? = null,
    val categoryId: String? = null,
    /** Human category label (Arabic) resolved server-side; null when uncategorised. */
    val categoryName: String? = null,
    val unit: String = "carton",
    val unitOfMeasure: String = "PCE",
    val price: Long = 0,                 // fils
    val cost: Long? = null,              // fils
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val reorderQty: Int = 0,
    val taxType: String = "TAXABLE",
    val taxCategory: String = "S",
    val taxRate: String = "0.1600",
    // ── Tobacco tax (from the item_cart row; default off if the BE omits them) ──
    val isTobaccoProduct: Boolean = false,
    val tobaccoTaxProfileId: String? = null,
    /** MSRP / consumer price for the tobacco base, integer fils per base piece.
     *  Nullable — the BE sends null for non-tobacco items. */
    val consumerPriceFils: Long? = null,
    /** The item's real sellable units (base + larger), from the dashboard/ERP. */
    val units: List<ProductUnitDto> = emptyList(),
)

/** One sellable unit of an item, as served by GET /products. */
@Serializable
data class ProductUnitDto(
    val name: String = "",
    val code: String = "",
    val conversionQty: Double = 1.0,
    val priceFils: Long = 0,
    val barcode: String = "",
    val isBase: Boolean = false,
    /** `item_units.id` — the authoritative unit identity a voucher line posts against.
     *  Blank on an old backend, which is why the app still synthesizes a fallback id. */
    val itemUnitId: String = "",
    /** The unit owns its stock pool (a colour/flavour variant) rather than converting
     *  into the item's base pool (a carton). */
    val isStockUnit: Boolean = false,
)

@Serializable
data class QuoteRequest(
    val qty: Double,
    val customerId: String? = null,
)

@Serializable
data class QuoteDto(
    val productId: String,
    val qty: Double,
    val segment: String? = null,
    val listUnitPrice: Long = 0,         // fils
    val appliedRuleId: String? = null,
    val discountPct: Double = 0.0,
    val finalUnitPrice: Long = 0,        // fils
    val lineTotal: Long = 0,             // fils
)
