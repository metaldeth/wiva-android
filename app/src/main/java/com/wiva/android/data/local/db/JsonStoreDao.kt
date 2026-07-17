package com.wiva.android.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface JsonStoreDao {
    @Query("SELECT * FROM json_data WHERE name = :name")
    suspend fun get(name: String): JsonStoreEntity?

    @Upsert
    suspend fun set(record: JsonStoreEntity)

    @Query("DELETE FROM json_data WHERE name = :name")
    suspend fun delete(name: String)

    @Query("SELECT * FROM json_data")
    suspend fun getAll(): List<JsonStoreEntity>
}
