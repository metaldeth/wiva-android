package com.viwa.android.data.repository

interface ConfigRepository {
    suspend fun get(key: String): String?
    suspend fun set(key: String, value: String)
    suspend fun delete(key: String)

    suspend fun getJson(key: String): String?
    suspend fun setJson(key: String, json: String)
}
