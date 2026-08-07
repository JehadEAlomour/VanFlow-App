package com.jehadalomour.flowvan.core.model

data class ProductUnit(
    /** The server's `itemUnitId` when it sent one — the identity a cart line and an
     *  upload post against. Falls back to a synthesized local id for old servers. */
    val id: String,
    val productId: String,
    val name: String,
    val price: Double,
    val conversionQty: Double,
    /** This unit's OWN stock pool, in base pieces. Only filled when [isStockUnit]. */
    val vanStock: Int = 0,
    /** The unit's server-side code — the stock pool key. */
    val code: String = "",
    /** The item's base unit. The server marks exactly one; the app never infers it. */
    val isBase: Boolean = false,
    /** A variant (own goods, own pool) rather than packaging over the base pool. */
    val isStockUnit: Boolean = false,
)
