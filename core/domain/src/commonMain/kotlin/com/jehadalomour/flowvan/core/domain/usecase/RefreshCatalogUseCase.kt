package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.api.ProductApi
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Server-first refresh on login / home: pulls customers + products, then overlays the rep's
 * per-rep van stock from `GET /reps/{repId}/van-stock` (the products endpoint carries no stock).
 * Refills Room so the UI stays offline-first.
 */
class RefreshCatalogUseCase(
    private val apiConfig: ApiConfig,
    private val customerApi: CustomerApi,
    private val productApi: ProductApi,
    private val repApi: RepApi,
    private val customers: CustomerRepository,
    private val products: ProductRepository,
    private val session: SessionStore,
) {
    private val log = Logger.withTag("RefreshCatalog")

    suspend operator fun invoke(): Result<CatalogRefresh> {
        if (!apiConfig.isEnabled) return Result.success(CatalogRefresh(0, 0, 0, skipped = true))
        return try {
            coroutineScope {
                val customersJob = async {
                    val page = customerApi.list()
                    customers.cacheAll(page.items.map { it.toEntity() })
                    page.items.size
                }
                // Products first (upsert replaces the row, zeroing stock)…
                val productCount = run {
                    val page = productApi.list()
                    products.cacheAll(page.items.map { it.toEntity() })
                    page.items.size
                }
                // …then overlay the real per-rep van stock.
                val stockCount = refreshVanStock()
                Result.success(CatalogRefresh(customersJob.await(), productCount, stockCount))
            }
        } catch (e: Exception) {
            log.e("Catalog refresh failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun refreshVanStock(): Int {
        val repId = session.currentRepId ?: return 0
        val stock = repApi.vanStock(repId)
        stock.forEach { products.setStock(it.productId, it.quantity) }
        return stock.size
    }
}

data class CatalogRefresh(
    val customers: Int,
    val products: Int,
    val vanStockItems: Int,
    val skipped: Boolean = false,
)
