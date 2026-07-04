package com.jehadalomour.flowvan.core.database.db

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun builder(): RoomDatabase.Builder<FlowVanDatabase>

    /**
     * Copies the live database into a `van-flow-backups` folder inside the app's Documents
     * directory, under [fileName] (plus its `-wal`/`-shm` sidecars for a consistent copy).
     * Returns the absolute path of the backup, or null if the source db is missing / the copy
     * failed. Runs synchronously — call it off the main thread.
     */
    fun backupToDocuments(fileName: String): String?
}