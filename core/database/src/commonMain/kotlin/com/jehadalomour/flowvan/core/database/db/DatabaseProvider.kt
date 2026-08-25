package com.jehadalomour.flowvan.core.database.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun buildFlowVanDatabase(factory: DatabaseFactory): FlowVanDatabase =
    factory.builder()
        .setDriver(BundledSQLiteDriver())
        // Only fires when the on-disk DB is NEWER than the code (a downgrade) —
        // e.g. a dev device left on a schema version whose feature was later
        // reverted. Normal forward migrations are untouched; this just rebuilds
        // the (re-syncable) cache instead of crashing on a missing N→N-1 path.
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()