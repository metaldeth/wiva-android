package com.viwa.android.data.local.sales

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class SalesOutboxStore
@Inject
constructor(
    private val configRepository: ConfigRepository,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    suspend fun enqueue(sale: PendingSale) {
        val current = loadAll()
        if (current.any { it.saleId == sale.saleId }) return
        save(current + sale.copy(status = PendingSaleStatus.PENDING))
    }

    suspend fun listPending(nowMillis: Long = System.currentTimeMillis()): List<PendingSale> =
        loadAll().filter {
            it.status == PendingSaleStatus.PENDING &&
                it.nextRetryAtMillis <= nowMillis
        }

    suspend fun markSent(saleId: String) {
        save(loadAll().filterNot { it.saleId == saleId })
    }

    suspend fun bumpAttempt(saleId: String, nowMillis: Long = System.currentTimeMillis()) {
        val updated =
            loadAll().map { sale ->
                if (sale.saleId != saleId) {
                    sale
                } else {
                    val nextAttempts = sale.attempts + 1
                    sale.copy(
                        attempts = nextAttempts,
                        nextRetryAtMillis = nowMillis + retryDelayMillis(nextAttempts),
                        status = PendingSaleStatus.PENDING,
                    )
                }
            }
        save(updated)
    }

    internal fun retryDelayMillis(attempts: Int): Long {
        val index = (attempts - 1).coerceAtLeast(0).coerceAtMost(RETRY_BACKOFF_MS.lastIndex)
        return RETRY_BACKOFF_MS[index]
    }

    private suspend fun loadAll(): List<PendingSale> {
        val raw = configRepository.getJson(JsonStoreKeys.PENDING_SALES) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PendingSale>>(raw)
        }.getOrElse {
            Timber.tag(TAG).e(it, "Failed to decode pending sales")
            emptyList()
        }
    }

    private suspend fun save(sales: List<PendingSale>) {
        val pendingOnly = sales.filter { it.status == PendingSaleStatus.PENDING }
        if (pendingOnly.isEmpty()) {
            configRepository.delete(JsonStoreKeys.PENDING_SALES)
            return
        }
        configRepository.setJson(JsonStoreKeys.PENDING_SALES, json.encodeToString(pendingOnly))
    }

    companion object {
        private const val TAG = "SalesOutboxStore"
        val RETRY_BACKOFF_MS = longArrayOf(1_000, 2_000, 5_000, 10_000, 30_000)
    }
}
