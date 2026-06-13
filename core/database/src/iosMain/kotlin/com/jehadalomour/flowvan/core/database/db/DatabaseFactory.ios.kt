package com.jehadalomour.flowvan.core.database.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual class DatabaseFactory {
    actual fun builder(): RoomDatabase.Builder<FlowVanDatabase> {
        val documentsUrl: NSURL = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("Unable to resolve documents directory")
        val dbPath = requireNotNull(documentsUrl.path) + "/" + FLOW_VAN_DB_NAME
        return Room.databaseBuilder<FlowVanDatabase>(name = dbPath)
    }
}