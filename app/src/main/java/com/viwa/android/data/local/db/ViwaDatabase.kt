package com.viwa.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [JsonStoreEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ViwaDatabase : RoomDatabase() {
    abstract fun jsonStoreDao(): JsonStoreDao
}
