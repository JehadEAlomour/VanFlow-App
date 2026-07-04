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

    actual fun backupToDocuments(fileName: String): String? {
        val fm = NSFileManager.defaultManager
        val docs = fm.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )?.path ?: return null
        val src = "$docs/$FLOW_VAN_DB_NAME"
        if (!fm.fileExistsAtPath(src)) return null
        val destDir = "$docs/$BACKUP_DIR"
        fm.createDirectoryAtPath(destDir, withIntermediateDirectories = true, attributes = null, error = null)
        // Copy the db plus its WAL/SHM sidecars so the backup opens as a consistent database.
        for ((suffix, outName) in listOf("" to fileName, "-wal" to "$fileName-wal", "-shm" to "$fileName-shm")) {
            val s = src + suffix
            if (fm.fileExistsAtPath(s)) {
                val d = "$destDir/$outName"
                if (fm.fileExistsAtPath(d)) fm.removeItemAtPath(d, error = null)
                fm.copyItemAtPath(s, toPath = d, error = null)
            }
        }
        return "$destDir/$fileName"
    }
}