package com.wiva.android.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "json_data")
data class JsonStoreEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "data") val data: String,
)
