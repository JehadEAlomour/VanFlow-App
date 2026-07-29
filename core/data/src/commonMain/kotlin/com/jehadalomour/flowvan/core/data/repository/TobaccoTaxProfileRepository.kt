package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.dao.TobaccoTaxProfileDao
import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.model.TobaccoTaxProfile
import com.jehadalomour.flowvan.core.network.api.TobaccoTaxProfileApi
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline cache of tobacco tax profiles (GET /tobacco-tax-profiles). [refresh] replaces the
 * cache; the voucher screen resolves a product's `tobaccoProfileId` against [observeAll].
 */
class TobaccoTaxProfileRepository(
    private val dao: TobaccoTaxProfileDao,
    private val api: TobaccoTaxProfileApi,
) {
    private val log = Logger.withTag("TobaccoTax")

    fun observeAll(): Flow<List<TobaccoTaxProfile>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** Fetch active profiles and replace the cache. Returns the number cached. */
    suspend fun refresh(): Result<Int> = runCatching {
        val dtos = api.list()
        dao.replaceAll(dtos.map { it.toEntity() })
        log.d { "tobacco profiles cached: ${dtos.size}" }
        dtos.size
    }.onFailure { log.w { "tobacco profiles refresh FAILED: ${it.message}" } }
}
