package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.network.api.AuthApi
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.api.OrderApi
import com.jehadalomour.flowvan.core.network.api.ProductApi
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.realtime.SyncResource
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.network.http.OffsetPage
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.OfferRepository
import com.jehadalomour.flowvan.core.data.repository.PriceListRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.TobaccoTaxProfileRepository
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
    private val orderApi: OrderApi,
    private val repApi: RepApi,
    private val customers: CustomerRepository,
    private val products: ProductRepository,
    private val productUnits: ProductUnitRepository,
    private val session: SessionStore,
    private val authApi: AuthApi,
    private val appSettings: AppSettingsRepository,
    private val offers: OfferRepository,
    private val priceLists: PriceListRepository,
    private val tobaccoProfiles: TobaccoTaxProfileRepository,
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
        // Cache each price list's item prices for offline per-customer pricing.
        priceLists.refresh().onFailure { log.w("price-lists refresh failed: ${it.message}") }
        // Cache tobacco tax profiles so tobacco items apply their excise/special tax at sale.
        tobaccoProfiles.refresh().onFailure { log.w("tobacco profiles refresh failed: ${it.message}") }
        return try {
            coroutineScope {
                val customersJob = async { refreshCustomers() }
                // Products first (upsert replaces the row, zeroing stock)…
                val productCount = refreshProducts()
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
     * Re-pull the product catalogue and each item's units.
     *
     * Extracted so a targeted ITEMS refresh and a full one leave identical local
     * state — the same reason [refreshCustomers] exists.
     *
     * Returns the number of products imported.
     */
    private suspend fun refreshProducts(): Int {
        // Page through ALL products so large catalogs (e.g. 1000 items)
        // import fully, not just the first page.
        val allItems = fetchAllPages { limit, offset -> productApi.list(limit = limit, offset = offset) }
        // REPLACE, not upsert: an item deleted in the ERP used to linger on the
        // device with its last-known stock, and since the catalog list filters on
        // vanStock > 0, those dead rows were the only ones a rep could see.
        products.replaceAll(allItems.map { it.toEntity() })
        // Cache the MAIN-STORE on-hand for the ORDER picker so it works offline.
        // Best-effort — a failure must not fail the catalogue refresh; the last cache
        // (or zero) stays. Base pool only; ORDER lines are base units.
        runCatching { orderApi.orderStock() }
            .onSuccess { rows ->
                products.cacheMainStock(
                    rows.filter { it.stockUnitCode.isEmpty() }
                        .associate { it.itemNumber to (it.itemQty.toDoubleOrNull()?.toInt() ?: 0) },
                )
            }
            .onFailure { log.w("main-store stock cache skipped: ${it.message}") }
        // …then refill each item's REAL units (base + larger) so the app
        // shows the item's own units, not a hardcoded list.
        val units = allItems.flatMap { p ->
            p.units.map { u ->
                // Trust the server's `isBase` flag. This used to also treat
                // "smallest conversionQty" as the base, which broke the moment a
                // product had SAME-SIZE units: a colour or flavour variant has
                // conversionQty 1 like the base does, so every one of them matched
                // and got renamed "حبة" — six colours showing as six identical
                // pieces, with no way for the rep to tell them apart. The backend
                // already marks exactly one unit isBase=true, so infer nothing.
                //
                // The rename still applies to a base whose ERP name is meaningless
                // in the field ("PCS", "Each"), but never overwrites a real name.
                ProductUnit(
                    // The server's item_units.id is the only stable identity: the old
                    // barcode-or-"$id:$code:$qty" expression collapsed every
                    // blank-barcode colour of an item onto ONE id, so six colours
                    // became one row. Kept only as the fallback for a backend that
                    // doesn't send itemUnitId yet.
                    id = u.itemUnitId.ifBlank {
                        u.barcode.ifBlank { "${p.id}:${u.code}:${u.conversionQty}" }
                    },
                    productId = p.id,
                    name = if (u.isBase) BASE_UNIT_NAME else u.name.ifBlank { BASE_UNIT_NAME },
                    price = u.priceFils / 1000.0,
                    conversionQty = u.conversionQty,
                    code = u.code,
                    isBase = u.isBase,
                    isStockUnit = u.isStockUnit,
                )
            }
        }
        // MERGE, never wipe: this refresh runs on login and on every home pull, and
        // the products endpoint carries no stock — a delete-then-insert zeroed every
        // variant pool each time. mergeAll keeps each surviving unit's vanStock.
        productUnits.mergeAll(units)
        return allItems.size
    }

    private suspend fun refreshCustomers(): Int {
        // Page through ALL customers (server caps each page at 200).
        val all = fetchAllPages { limit, offset -> customerApi.list(limit = limit, offset = offset) }
        // Full replace (not upsert): the server list is scoped to this
        // salesman, so customers no longer assigned to them are removed.
        customers.replaceAll(all.map { it.toEntity() })
        return all.size
    }

    /**
     * Re-pull ONE slice, in response to a realtime signal from the server.
     *
     * Deliberately narrow: a signal that offers changed should not drag the whole
     * product catalog over a patchy mobile connection. Each branch mirrors the
     * matching part of [invoke], so a targeted refresh and a full one leave the
     * same local state.
     *
     * Best-effort by contract — the caller is a socket event, not a user waiting
     * on a result. A failure leaves the previous cache in place and the next
     * foreground refresh corrects it.
     */
    suspend fun refreshOnly(resource: SyncResource): Result<Unit> {
        if (!apiConfig.isEnabled) return Result.success(Unit)
        return runCatching {
            when (resource) {
                SyncResource.OFFERS -> offers.refresh().getOrThrow()
                SyncResource.CUSTOMERS -> refreshCustomers()
                SyncResource.STOCK -> refreshVanStock()
                // Products, then units, then van stock — in that order, and the
                // stock overlay is NOT optional. replaceAll() rewrites each product
                // row and zeroes its quantity, so an items-only refresh without the
                // overlay would leave every product at 0 on the device and the
                // catalog screen (which filters on vanStock > 0) would go empty.
                // Price lists come too: an ERP price change lands there, not on the
                // product row, and it is what the rep actually quotes.
                SyncResource.ITEMS -> {
                    refreshProducts()
                    refreshVanStock()
                    priceLists.refresh().getOrThrow()
                }
            }
            Unit
        }.onFailure { log.w("targeted refresh ($resource) failed: ${it.message}") }
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

    /**
     * Overlay the rep's stock, one row per POOL. A base-pool row (`stockUnitCode` blank) sets
     * `products.vanStock` exactly as before; a variant row sets that unit's own pool instead,
     * so red and blue no longer share one number. A variant row we cannot resolve to a local
     * unit is skipped rather than folded into the base pool — folding it would let the rep
     * sell blue against red's stock.
     */
    private suspend fun refreshVanStock(): Int {
        val repId = session.currentRepId ?: return 0
        val stock = repApi.vanStock(repId)
        stock.forEach { row ->
            val poolCode = row.stockUnitCode.orEmpty()
            if (poolCode.isBlank()) {
                products.setStock(row.productId, row.quantity)
            } else {
                val unitId = row.itemUnitId.orEmpty().ifBlank {
                    productUnits.findByProductAndCode(row.productId, poolCode)?.id.orEmpty()
                }
                if (unitId.isNotBlank()) productUnits.setStock(unitId, row.quantity)
                else log.w("van-stock row for unresolved unit $poolCode on ${row.productId}")
            }
        }
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
            session.canAddCustomer = me.permissions["canAddCustomer"] == true
            session.canCreateCustomerDirect = me.permissions["canCreateCustomerDirect"] == true
            session.canFindCustomers = me.permissions["canFindCustomers"] == true
            session.routesOnly = me.permissions["routesOnly"] == true
            session.canPrintLineDiscount = me.permissions["canPrintLineDiscount"] == true
            // Opt-out like login: absent key stays allowed, explicit false hides the tile.
            session.canCreateSale = me.permissions["canCreateSale"] != false
            session.canCreateReturn = me.permissions["canCreateReturn"] != false
            session.canMakeCollection = me.permissions["canMakeCollection"] != false
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
