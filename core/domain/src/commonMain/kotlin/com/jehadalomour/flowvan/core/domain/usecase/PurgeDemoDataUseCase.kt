package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.db.FlowVanDatabase
import com.jehadalomour.flowvan.core.datastore.SettingsKeys
import com.russhwolf.settings.Settings

/**
 * One-time wipe of locally-seeded demo rows on existing installs (the demo seeder has been
 * removed — the app now sources its data from the VanFlow backend). Runs once, guarded by
 * the [SettingsKeys.DEMO_PURGED] flag; a no-op on every launch after that.
 *
 * Only the previously-seeded tables are cleared. App settings, AI messages, route stops and
 * location points are left untouched.
 */
class PurgeDemoDataUseCase(
    private val db: FlowVanDatabase,
    private val settings: Settings,
) {
    private val log = Logger.withTag("PurgeDemoData")

    suspend operator fun invoke() {
        if (settings.getBoolean(SettingsKeys.DEMO_PURGED, false)) return
        runCatching {
            db.invoiceDao().deleteAll()
            db.paymentDao().deleteAll()
            db.productUnitDao().deleteAll()
            db.productDao().deleteAll()
            db.customerDao().deleteAll()
            db.shiftDao().deleteAll()
            db.userDao().deleteAll()
        }.onFailure { log.e("demo purge failed: ${it.message}") }
        settings.putBoolean(SettingsKeys.DEMO_PURGED, true)
        // Clear the legacy seeded flag so nothing tries to re-seed.
        settings.remove(SettingsKeys.DEMO_SEEDED)
        log.i { "local demo data purged" }
    }
}
