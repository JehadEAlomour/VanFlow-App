package com.jehadalomour.flowvan.shared.data.local.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun buildFlowVanDatabase(factory: DatabaseFactory): FlowVanDatabase =
    factory.builder()
        .setDriver(BundledSQLiteDriver())
        .build()