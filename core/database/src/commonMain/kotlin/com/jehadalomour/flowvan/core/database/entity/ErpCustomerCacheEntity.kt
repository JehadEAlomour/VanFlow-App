package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last-known ERP money for one customer, so the app can show the ERP's own
 * balance and statement — live when online, this cached copy (with its [asOfMillis]
 * "as of" time) when offline.
 *
 * [balance]/[creditLimit] are JOD major units (as the ERP serves them). [statementJson]
 * is the serialized [com.jehadalomour.flowvan.core.network.dto.ErpStatementDto] for the
 * last successful fetch, parsed on read. [available] is false when the last fetch came
 * back unavailable (unlinked / erp_off / not_found) — the row still exists so the "as of"
 * time and the reason survive offline.
 */
@Entity(tableName = "erp_customer_cache")
data class ErpCustomerCacheEntity(
    @PrimaryKey val customerId: String,
    val available: Boolean,
    val reason: String?,
    val balance: Double?,
    val creditLimit: Double?,
    val statementJson: String?,
    /** Epoch-ms of the refresh that wrote this row — the statement/balance "as of" time. */
    val asOfMillis: Long,
)
