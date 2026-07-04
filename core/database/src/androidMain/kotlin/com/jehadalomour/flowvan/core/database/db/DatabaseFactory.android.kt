package com.jehadalomour.flowvan.core.database.db

import android.content.Context
import android.os.Environment
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class DatabaseFactory(private val context: Context) {
    actual fun builder(): RoomDatabase.Builder<FlowVanDatabase> {
        val dbFile = context.getDatabasePath(FLOW_VAN_DB_NAME)
        return Room.databaseBuilder<FlowVanDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath,
            factory = { FlowVanDatabaseConstructor.initialize() },
        )
    }

    actual fun backupToDocuments(fileName: String): String? = runCatching {
        val src = context.getDatabasePath(FLOW_VAN_DB_NAME)
        if (!src.exists()) return null
        // App-scoped external Documents dir — accessible via file managers, needs no permission.
        val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val destDir = File(docsDir, BACKUP_DIR).apply { mkdirs() }
        val dest = File(destDir, fileName)
        src.copyTo(dest, overwrite = true)
        // Copy the WAL/SHM sidecars alongside so the backup opens as a consistent database.
        for (suffix in arrayOf("-wal", "-shm")) {
            val side = File(src.parentFile, FLOW_VAN_DB_NAME + suffix)
            if (side.exists()) side.copyTo(File(destDir, fileName + suffix), overwrite = true)
        }
        dest.absolutePath
    }.getOrNull()
}