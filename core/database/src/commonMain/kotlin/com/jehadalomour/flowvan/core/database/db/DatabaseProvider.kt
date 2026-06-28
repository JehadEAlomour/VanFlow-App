package com.jehadalomour.flowvan.core.database.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun buildFlowVanDatabase(factory: DatabaseFactory): FlowVanDatabase =
    factory.builder()
        .setDriver(BundledSQLiteDriver())
        .build()