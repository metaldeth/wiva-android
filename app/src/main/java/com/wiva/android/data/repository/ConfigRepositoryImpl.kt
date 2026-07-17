package com.wiva.android.data.repository

import com.wiva.android.data.local.db.JsonStoreDao
import com.wiva.android.data.local.db.JsonStoreEntity
import javax.inject.Inject

class ConfigRepositoryImpl
@Inject
constructor(
    private val dao: JsonStoreDao,
) : ConfigRepository {
    override suspend fun get(key: String): String? = dao.get(key)?.data

    override suspend fun set(key: String, value: String) = dao.set(JsonStoreEntity(key, value))

    override suspend fun delete(key: String) = dao.delete(key)

    override suspend fun getJson(key: String): String? = dao.get(key)?.data

    override suspend fun setJson(key: String, json: String) = dao.set(JsonStoreEntity(key, json))
}
