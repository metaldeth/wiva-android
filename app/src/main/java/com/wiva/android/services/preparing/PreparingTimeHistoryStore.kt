package com.wiva.android.services.preparing

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class PreparingTimeRecord(
    val timestampEpochMs: Long,
    val tasteId: Int,
    val drinkName: String,
    val containerNumber: Int,
    val volumeMl: Int,
    val recipeDrinkVolumeMl: Int,
    val recipeWaterMl: Double,
    val actualWaterMl: Double,
    val flowRateMlPerSec: Double,
    val expectedTimeSec: Int,
    val actualTimeSec: Double,
)

@Singleton
class PreparingTimeHistoryStore
@Inject
constructor(
    private val configRepository: ConfigRepository,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    suspend fun loadAll(): List<PreparingTimeRecord> {
        val raw = configRepository.getJson(JsonStoreKeys.PREPARING_TIME_HISTORY) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PreparingTimeRecord>>(raw)
        }.getOrElse {
            Timber.tag(TAG).e(it, "Failed to decode preparing time history")
            emptyList()
        }
    }

    suspend fun append(record: PreparingTimeRecord) {
        val current = loadAll()
        val updated = (current + record).takeLast(MAX_RECORDS)
        val encoded = json.encodeToString(updated)
        configRepository.setJson(JsonStoreKeys.PREPARING_TIME_HISTORY, encoded)
    }

    companion object {
        private const val TAG = "PreparingTimeHistory"
        private const val MAX_RECORDS = 500
    }
}
