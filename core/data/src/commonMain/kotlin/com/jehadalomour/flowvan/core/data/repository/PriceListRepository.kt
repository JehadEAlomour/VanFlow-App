package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.dao.PriceListItemDao
import com.jehadalomour.flowvan.core.database.entity.PriceListItemEntity
import com.jehadalomour.flowvan.core.network.api.PriceListApi

/**
 * Offline cache of price-list item prices (from GET /price-lists/full). [refresh]
 * replaces the whole cache; [pricesForList] loads one customer's list into a
 * sku→price map when a customer is opened, so the voucher screen resolves each
 * item's price to the contracted price (falling back to the base catalog price).
 * Prices are stored as base-unit JOD.
 */
class PriceListRepository(
    private val dao: PriceListItemDao,
    private val api: PriceListApi,
) {
    private val log = Logger.withTag("PriceLists")

    /** Fetch all lists + items and replace the cache. Returns rows cached. */
    suspend fun refresh(): Result<Int> = runCatching {
        val lists = api.full()
        val rows = lists.flatMap { list ->
            list.items.mapNotNull { item ->
                val sku = item.itemNumber?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                PriceListItemEntity(
                    priceListId = list.id,
                    sku = sku,
                    unitPrice = item.unitPrice / 1000.0, // fils → JOD
                )
            }
        }
        dao.replaceAll(rows)
        log.d { "price-lists cache refreshed: ${rows.size} item prices across ${lists.size} lists" }
        rows.size
    }.onFailure { log.w { "price-lists cache refresh FAILED: ${it.message}" } }

    /** Base-unit price for one product under a list, or null when it isn't on the list. */
    suspend fun priceFor(priceListId: String, sku: String): Double? = dao.priceFor(priceListId, sku)

    /** A customer's whole list as sku→base-unit-price (JOD). Empty when unassigned. */
    suspend fun pricesForList(priceListId: String?): Map<String, Double> {
        if (priceListId.isNullOrBlank()) return emptyMap()
        return dao.listFor(priceListId).associate { it.sku to it.unitPrice }
    }
}
