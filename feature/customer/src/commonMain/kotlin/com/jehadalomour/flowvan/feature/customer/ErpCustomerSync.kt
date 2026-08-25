package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.core.data.repository.ErpFinanceRepository
import com.jehadalomour.flowvan.core.database.entity.ErpCustomerCacheEntity
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.dto.ErpStatementDto
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Pulls a customer's ERP balance + statement and writes them into the offline
 * cache. The one place the network→cache step lives, shared by the customer
 * dashboard and the statement screen so they can't drift apart.
 *
 * Offline-safe: if both calls fail nothing is written (the last-known row, with
 * its "as of" time, stays). Partial connectivity merges field-by-field against
 * the prior row so a failed statement never wipes a good cached one, and vice
 * versa.
 */
@OptIn(ExperimentalTime::class)
class ErpCustomerSync(
    private val customerApi: CustomerApi,
    private val erpFinance: ErpFinanceRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun refresh(customerId: String, from: String? = null, to: String? = null) {
        val balance = runCatching { customerApi.erpBalance(customerId) }.getOrNull()
        val statement = runCatching { customerApi.erpStatement(customerId, from, to) }.getOrNull()
        if (balance == null && statement == null) return // offline — keep the cached row

        val prior = erpFinance.getCustomer(customerId)
        val statementOk = statement?.isAvailable == true
        val statementJson = if (statementOk) {
            json.encodeToString(ErpStatementDto.serializer(), statement!!)
        } else {
            prior?.statementJson // a failed/unavailable statement keeps the last good one
        }
        val newBalance = balance?.takeIf { it.source == "erp" }?.balance ?: prior?.balance
        val newLimit = balance?.creditLimit
            ?: statement?.creditLimit
            ?: prior?.creditLimit
        // "Available" if we hold any real ERP figure now — freshly fetched or still cached.
        val available = balance?.source == "erp" || statementOk ||
            (balance == null && statement == null && prior?.available == true)
        val reason = if (available) null else (balance?.reason ?: statement?.reason ?: prior?.reason)

        erpFinance.cacheCustomer(
            ErpCustomerCacheEntity(
                customerId = customerId,
                available = available,
                reason = reason,
                balance = newBalance,
                creditLimit = newLimit,
                statementJson = statementJson,
                asOfMillis = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }
}
