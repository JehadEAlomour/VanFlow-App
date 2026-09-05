package com.jehadalomour.flowvan.core.model

data class Product(
    val id: String,
    val sku: String,
    val nameAr: String,
    val nameEn: String,
    val category: String,
    val unit: String,
    val salePrice: Double,
    val costPrice: Double,
    val vanStock: Int,
    /** Main-store (central depot) on-hand, cached from the ERP for the ORDER flow — works offline. */
    val mainStock: Int = 0,
    val minStock: Int,
    val expiryDate: Long?,
    val brand: String?,
    val taxRate: Double = 0.16,
    val imageUrl: String? = null,
    // ── Tobacco tax (mirrors the ERP/cash-van item tobacco fields) ──────────────
    /** When true this item is taxed via a tobacco profile, not normal GST. */
    val isTobacco: Boolean = false,
    /** The tobacco tax profile id to apply (resolve against the synced profiles). */
    val tobaccoProfileId: String? = null,
    /** MSRP / consumer price used as a tobacco tax base, in integer fils per base piece. */
    val consumerPriceFils: Long = 0L,
)