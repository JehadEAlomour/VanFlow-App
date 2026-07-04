package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.dao.OfferDao
import com.jehadalomour.flowvan.core.model.OfferDefinition
import com.jehadalomour.flowvan.core.network.api.OfferApi
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Offline cache of active offers for on-device evaluation. [refresh] pulls GET /offers/active
 * and replaces the cache; reads parse the raw JSON blobs into engine-ready [OfferDefinition]s,
 * dropping any that don't parse (unknown type / malformed config).
 *
 * The cache is deliberately customer-agnostic (the endpoint applies only schedule filtering,
 * not eligibility) — eligibility and limits are gated locally by the evaluator.
 */
class OfferRepository(
    private val dao: OfferDao,
    private val api: OfferApi,
    private val json: Json,
) {
    private val log = Logger.withTag("Offers")
    /** Active, parseable offers in the server ranking order (priority DESC, createdAt ASC). */
    suspend fun activeOffers(): List<OfferDefinition> =
        dao.listActive().mapNotNull { it.toDefinition(json) }

    fun observeActive(): Flow<List<OfferDefinition>> =
        dao.observeActive().map { rows -> rows.mapNotNull { it.toDefinition(json) } }

    /** Epoch-ms of the most recent successful refresh, or null if the cache is empty. */
    suspend fun lastRefreshedAt(): Long? = dao.lastRefreshedAt()

    /** Fetch the active offers and replace the whole cache. Returns the number cached. */
    suspend fun refresh(storeNumber: String? = null): Result<Int> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val dtos = api.activeOffers(customerNumber = null, storeNumber = storeNumber)
        dao.replaceAll(dtos.map { it.toEntity(now) })
        log.d { "offers cache refreshed: ${dtos.size} active offers stored" }
        dtos.size
    }.onFailure { log.w { "offers cache refresh FAILED: ${it.message}" } }
}
