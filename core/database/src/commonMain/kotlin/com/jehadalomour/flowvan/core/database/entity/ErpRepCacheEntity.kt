package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last-known ERP balance for the signed-in rep — the linked "cash with salesman"
 * GL account, shown live when online and from this cached copy (with its
 * [asOfMillis] "as of" time) when offline.
 *
 * [balance] is JOD major units. [available] is false when the last fetch came back
 * unavailable (rep not ERP-linked / ERP off) — the row still records the reason and
 * the "as of" time.
 */
@Entity(tableName = "erp_rep_cache")
data class ErpRepCacheEntity(
    @PrimaryKey val repId: String,
    val available: Boolean,
    val reason: String?,
    val balance: Double?,
    val accountName: String?,
    /** Epoch-ms of the refresh that wrote this row — the balance "as of" time. */
    val asOfMillis: Long,
)
