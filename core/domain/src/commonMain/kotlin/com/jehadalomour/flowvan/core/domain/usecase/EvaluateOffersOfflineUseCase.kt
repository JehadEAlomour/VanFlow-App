package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.OfferRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.domain.offers.LocalOfferEvaluator
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferSource
import com.jehadalomour.flowvan.core.network.mapper.toOfferEvaluation
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

/**
 * Offline offers evaluation: runs the cached offers through the on-device [LocalOfferEvaluator]
 * so the rep still sees discounts/gifts with no connection. Loads unit prices + tax from the
 * product cache and eligibility fields from the customer cache; the result is tagged
 * [OfferSource.LOCAL] (provisional — the server re-applies on sync).
 */
class EvaluateOffersOfflineUseCase(
    private val offers: OfferRepository,
    private val products: ProductRepository,
    private val customers: CustomerRepository,
    private val invoiceDao: InvoiceDao,
) {
    private val log = Logger.withTag("Offers")

    suspend operator fun invoke(
        cart: List<CartLine>,
        customerId: String?,
        repId: String?,
        storeNumber: String?,
        paymentMethod: String?,
        chosenFreeItems: List<String>,
    ): OfferEvaluation {
        if (cart.isEmpty()) return OfferEvaluation.EMPTY

        // No cached offers → still a LOCAL result (offline, no discounts) so the UI shows the
        // offline banner and on-device totals rather than the drop-everything fallback.
        val activeOffers = offers.activeOffers()
        log.d { "offline eval: cart=${cart.map { it.sku + "x" + it.qty }} cachedOffers=${activeOffers.size} pay=$paymentMethod" }
        if (activeOffers.isEmpty()) {
            log.w { "offline eval: OFFERS CACHE IS EMPTY — go online once so GET /offers/active can cache them" }
            return OfferEvaluation.EMPTY.copy(source = OfferSource.LOCAL)
        }

        // Price cart items from their OWN cart line (its unit price + tax) so the offline
        // per-line totals match exactly what the rep sees on the cart — the server re-derives
        // from the catalog on sync. Gift-pool items not in the cart fall back to the catalog
        // (a free line shows the gifted item at its real price).
        val giftSkus = activeOffers
            .mapNotNull { it.reward as? OfferReward.Gift }
            .flatMap { it.giftItems }
        val items = buildMap {
            for (line in cart) {
                put(line.sku, LocalOfferEvaluator.ItemInfo(priceFils = line.unitPrice.toFils(), taxPct = line.taxRate * 100.0))
            }
            for (sku in giftSkus) {
                if (sku in this) continue
                val p = products.findBySku(sku) ?: continue
                put(sku, LocalOfferEvaluator.ItemInfo(priceFils = p.salePrice.toFils(), taxPct = p.taxRate * 100.0))
            }
        }

        val customer = customerId?.let { customers.findById(it) }

        val needsNew = activeOffers.any { it.eligibility.customerScope == "NEW_ONLY" }
        val isNew = needsNew && customerId != null && invoiceDao.countSalesByCustomer(customerId) == 0

        val now = Clock.System.now()
        val ctx = LocalOfferEvaluator.Context(
            customerNumber = customer?.code,
            customerCategory = customer?.category,
            customerRegionId = customer?.regionId,
            customerRepId = customer?.repId,
            repId = repId,
            storeNumber = storeNumber,
            paymentMethod = paymentMethod,
            chosenFreeItems = chosenFreeItems,
            isNewCustomer = isNew,
            at = now.toLocalDateTime(TimeZone.currentSystemDefault()),
            nowMs = now.toEpochMilliseconds(),
        )

        val result = LocalOfferEvaluator
            .evaluate(cart.map { it.sku to it.qty }, activeOffers, items, ctx)
            .toOfferEvaluation()
            .copy(source = OfferSource.LOCAL)
        log.d { "offline eval result: appliedOffers=${result.appliedOffers.map { it.name }} freeChoices=${result.pendingChoices.size} lineDiscount=${result.totals.totalDiscountJod}" }
        return result
    }

    private fun Double.toFils(): Long = (this * 1000.0).roundToLong()
}
