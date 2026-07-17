package com.wiva.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [JsonStoreEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WivaDatabase : RoomDatabase() {
    abstract fun jsonStoreDao(): JsonStoreDao
}
