package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.network.api.AuthApi
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.api.ProductApi
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.OfferRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.ProductUnit
import com.jehadalomour.flowvan.core.model.TaxType
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
    private val productUnits: ProductUnitRepository,
    private val session: SessionStore,
    private val authApi: AuthApi,
    private val appSettings: AppSettingsRepository,
    private val offers: OfferRepository,
) {
    private val log = Logger.withTag("RefreshCatalog")

    private companion object {
        /** Canonical name for every item's base/piece unit. */
        const val BASE_UNIT_NAME = "حبة"
    }

    suspend operator fun invoke(): Result<CatalogRefresh> {
        if (!apiConfig.isEnabled) return Result.success(CatalogRefresh(0, 0, 0, skipped = true))
        syncCompanyTaxMode()
        syncPermissions()
        // Refresh the offline offers cache alongside the catalog. Best-effort: a failure
        // here must not fail the catalog refresh (the previous cache stays usable).
        offers.refresh().onFailure { log.w("offers refresh failed: ${it.message}") }
        return try {
            coroutineScope {
                val customersJob = async {
                    // Page through ALL customers (server caps each page at 200).
                    val all = fetchAllPages { limit, offset -> customerApi.list(limit = limit, offset = offset) }
                    customers.cacheAll(all.map { it.toEntity() })
                    all.size
                }
                // Products first (upsert replaces the row, zeroing stock)…
                val productCount = run {
                    // Page through ALL products so large catalogs (e.g. 1000 items)
                    // import fully, not just the first page.
                    val allItems = fetchAllPages { limit, offset -> productApi.list(limit = limit, offset = offset) }
                    products.cacheAll(allItems.map { it.toEntity() })
                    // …then refill each item's REAL units (base + larger) so the app
                    // shows the item's own units, not a hardcoded list.
                    val units = allItems.flatMap { p ->
                        // The base unit (smallest conversion) is always labelled "حبة" (piece),
                        // regardless of what the ERP names it; larger units keep their own names.
                        val baseConv = p.units.minOfOrNull { it.conversionQty }
                        p.units.map { u ->
                            val isBaseUnit = u.isBase || u.conversionQty == baseConv
                            ProductUnit(
                                id = u.barcode.ifBlank { "${p.id}:${u.code}:${u.conversionQty}" },
                                productId = p.id,
                                name = if (isBaseUnit) BASE_UNIT_NAME else u.name,
                                price = u.priceFils / 1000.0,
                                conversionQty = u.conversionQty,
                            )
                        }
                    }
                    productUnits.deleteAll()
                    if (units.isNotEmpty()) productUnits.upsertAll(units)
                    allItems.size
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

    /**
     * Drains every page of an offset/limit list endpoint (the server caps each
     * page at 200) so the whole catalog imports — not just the first 200 rows.
     * Stops when a short/empty page arrives or the running count reaches `total`.
     */
    private suspend fun <T> fetchAllPages(
        pageSize: Int = 200,
        fetch: suspend (limit: Int, offset: Int) -> OffsetPage<T>,
    ): List<T> {
        val all = mutableListOf<T>()
        var offset = 0
        while (true) {
            val page = fetch(pageSize, offset)
            all += page.items
            offset += page.items.size
            if (page.items.size < pageSize || (page.total > 0 && offset >= page.total)) break
            if (page.items.isEmpty()) break
            if (offset > 100_000) break // hard safety cap
        }
        return all
    }

    private suspend fun refreshVanStock(): Int {
        val repId = session.currentRepId ?: return 0
        val stock = repApi.vanStock(repId)
        stock.forEach { products.setStock(it.productId, it.quantity) }
        return stock.size
    }

    /**
     * Refresh the salesman's permissions from the server (`GET /auth/me`) so any
     * change an admin made on the dashboard takes effect on the next home refresh
     * — no re-login needed. Best-effort. Stored as the comma-joined permKeys that
     * `SessionStore.can(...)` reads.
     */
    private suspend fun syncPermissions() {
        try {
            val me = authApi.me()
            session.currentPermKeys = me.permKeys.joinToString(",")
        } catch (e: Exception) {
            log.w("permissions sync failed: ${e.message}")
        }
    }

    /**
     * Pull the company tax mode from the server (which mirrors the ERP, the source
     * of truth) and apply it locally, so the app's offline money engine always uses
     * the SAME inclusive/exclusive mode as the dashboard + ERP. Best-effort.
     */
    private suspend fun syncCompanyTaxMode() {
        try {
            val info = authApi.companyInfo()
            val mode =
                if (info.taxCalcMethod.equals("INCLUSIVE", ignoreCase = true)) TaxType.INCLUDED_TAX
                else TaxType.EXCLUDED_TAX
            val current = appSettings.get()
            if (current.taxType != mode) appSettings.save(current.copy(taxType = mode))
        } catch (e: Exception) {
            log.w("company tax-mode sync failed: ${e.message}")
        }
    }
}

data class CatalogRefresh(
    val customers: Int,
    val products: Int,
    val vanStockItems: Int,
    val skipped: Boolean = false,
)
