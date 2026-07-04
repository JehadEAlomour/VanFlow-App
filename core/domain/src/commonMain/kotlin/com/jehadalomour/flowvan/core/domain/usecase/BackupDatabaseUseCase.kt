package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.db.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Writes a dated copy of the local database into the Documents folder. Called on every login so
 * there is a rolling on-device backup. The file is named e.g. `van-flow-23-3-2026-14-05-09.db`
 * (day-month-year-hour-minute-second) so each login produces a distinct, ordered snapshot.
 *
 * Best-effort: any failure is logged and swallowed so it never blocks the login flow.
 */
class BackupDatabaseUseCase(
    private val databaseFactory: DatabaseFactory,
) {
    private val log = Logger.withTag("DbBackup")

    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): String? = withContext(Dispatchers.Default) {
        runCatching {
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.currentSystemDefault())
            fun pad(v: Int) = v.toString().padStart(2, '0')
            val name = "van-flow-${now.dayOfMonth}-${now.monthNumber}-${now.year}-" +
                "${pad(now.hour)}-${pad(now.minute)}-${pad(now.second)}.db"
            databaseFactory.backupToDocuments(name)?.also { path ->
                log.i { "database backed up to $path" }
            }
        }.onFailure { log.w(it) { "database backup failed" } }.getOrNull()
    }
}
