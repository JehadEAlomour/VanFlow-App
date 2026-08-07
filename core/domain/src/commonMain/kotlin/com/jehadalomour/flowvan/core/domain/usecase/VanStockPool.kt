package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.ProductUnit

/**
 * Which van pool a cart line moves.
 *
 * A VARIANT unit (أحمر — its own goods) owns its pool, held on `product_units.van_stock`.
 * Everything else — the base unit and any packaging unit (كرتونة ×12) — moves the item's
 * base pool on `products.vanStock`, exactly as before this feature existed. Quantities are
 * base pieces on both sides, so only the key differs, never the arithmetic.
 *
 * Returns null when the line draws from the item's base pool.
 */
internal suspend fun CartLine.stockUnit(productUnits: ProductUnitRepository): ProductUnit? =
    unitId.takeIf { it.isNotBlank() }
        ?.let { productUnits.findById(it) }
        ?.takeIf { it.isStockUnit }

/** Move [delta] base pieces in whichever pool [line] belongs to (see [stockUnit]). */
internal suspend fun applyVanStockDelta(
    products: ProductRepository,
    productUnits: ProductUnitRepository,
    line: CartLine,
    delta: Int,
) {
    val unit = line.stockUnit(productUnits)
    if (unit != null) productUnits.adjustStock(unit.id, delta)
    else products.adjustStock(line.productId, delta)
}
