package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.model.giftedBaseQtyBySku

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

/**
 * One van pool a voucher draws on: what the van holds and what the voucher takes out of
 * it, both in base pieces. [unitId] is set only for a variant unit's own pool.
 */
internal class PoolDemand(
    val productId: String,
    val unitId: String?,
    val available: Int,
    val requestedBase: Int,
)

/**
 * Every pool this sale moves, tallied ONCE per pool: the sold lines plus the offers'
 * free lines.
 *
 * Two things used to be missed by checking each cart line on its own. Lines that share a
 * pool (the base unit and a كرتونة ×12 of the same item) were each compared with the full
 * stock, so two lines of six passed against six pieces. And gifts were not counted at
 * all — see [giftedBaseQtyBySku] — so "buy 6 get 1 free" off the rep's last six was
 * saved locally and then rejected on sync.
 *
 * A gift SKU that isn't in the local catalogue contributes nothing: there is no pool to
 * check it against, and refusing a sale over an item we know nothing about is worse than
 * letting the server have the last word on it.
 */
internal suspend fun saleStockDemand(
    products: ProductRepository,
    productUnits: ProductUnitRepository,
    cart: List<CartLine>,
    freeLines: List<FreeLine>,
): List<PoolDemand> {
    class Acc(val productId: String, val unitId: String?, val available: Int) {
        var requestedBase = 0.0
    }
    val pools = LinkedHashMap<String, Acc>()

    fun add(productId: String, unitId: String?, available: Int, baseQty: Double) {
        val key = unitId?.let { "unit:$it" } ?: "product:$productId"
        pools.getOrPut(key) { Acc(productId, unitId, available) }.requestedBase += baseQty
    }

    // Which product row each sku is being SOLD under, so a gift of an item already in the
    // cart lands in the pool that cart line moves. The two can disagree — the offers
    // engine keys everything by item number, and a sku can resolve to a different product
    // row than the line the rep built — and a gift charged to the wrong pool is a gift
    // that isn't checked at all.
    val soldProductIdBySku = mutableMapOf<String, String>()

    for (line in cart) {
        val product = products.findById(line.productId)
            ?: error("product ${line.productId} not found")
        val unit = line.stockUnit(productUnits)
        if (unit == null) soldProductIdBySku.putIfAbsent(line.sku, line.productId)
        add(line.productId, unit?.id, unit?.vanStock ?: product.vanStock, line.stockQty)
    }
    for ((sku, giftedBase) in freeLines.giftedBaseQtyBySku()) {
        val productId = soldProductIdBySku[sku] ?: products.findBySku(sku)?.id ?: continue
        val product = products.findById(productId) ?: continue
        add(productId, null, product.vanStock, giftedBase)
    }

    return pools.values.map {
        PoolDemand(it.productId, it.unitId, it.available, it.requestedBase.toInt())
    }
}

/** Move [delta] base pieces in the pool [demand] identifies (see [saleStockDemand]). */
internal suspend fun applyVanStockDelta(
    products: ProductRepository,
    productUnits: ProductUnitRepository,
    demand: PoolDemand,
    delta: Int,
) {
    if (demand.unitId != null) productUnits.adjustStock(demand.unitId, delta)
    else products.adjustStock(demand.productId, delta)
}
