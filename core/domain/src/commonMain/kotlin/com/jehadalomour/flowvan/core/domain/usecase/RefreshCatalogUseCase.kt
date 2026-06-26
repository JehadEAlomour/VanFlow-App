package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.network.api.AuthApi
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.api.ProductApi
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
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
) {
    private val log = Logger.withTag("RefreshCatalog")

    suspend operator fun invoke(): Result<CatalogRefresh> {
        if (!apiConfig.isEnabled) return Result.success(CatalogRefresh(0, 0, 0, skipped = true))
        syncPermissions()
        syncCompanyTaxMode()
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
                    // …then refill each item's REAL units (base + larger) so the app
                    // shows the item's own units, not a hardcoded list.
                    val units = page.items.flatMap { p ->
                        p.units.map { u ->
                            ProductUnit(
                                id = u.barcode.ifBlank { "${p.id}:${u.code}:${u.conversionQty}" },
                                productId = p.id,
                                name = u.name,
                                price = u.priceFils / 1000.0,
                                conversionQty = u.conversionQty,
                            )
                        }
                    }
                    productUnits.deleteAll()
                    if (units.isNotEmpty()) productUnits.upsertAll(units)
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
