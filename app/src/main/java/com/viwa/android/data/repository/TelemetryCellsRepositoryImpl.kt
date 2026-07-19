package com.viwa.android.data.repository

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.domain.model.TelemetryCellsSnapshot
import com.viwa.android.domain.repository.TelemetryCellsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class TelemetryCellsRepositoryImpl
@Inject
constructor(
    private val configRepository: ConfigRepository,
) : TelemetryCellsRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    private val mutex = Mutex()
    private val _snapshotFlow = MutableStateFlow<TelemetryCellsSnapshot?>(null)
    override val snapshotFlow: StateFlow<TelemetryCellsSnapshot?> = _snapshotFlow.asStateFlow()

    override suspend fun getSnapshot(): TelemetryCellsSnapshot? =
        mutex.withLock {
            _snapshotFlow.value ?: loadFromStore()?.also { _snapshotFlow.value = it }
        }

    override suspend fun replaceSnapshot(snapshot: TelemetryCellsSnapshot) {
        mutex.withLock {
            val persisted = snapshot.copy(savedAtEpochMs = System.currentTimeMillis())
            saveToStore(persisted)
            _snapshotFlow.value = persisted
        }
    }

    override suspend fun clearSnapshot() {
        mutex.withLock {
            configRepository.delete(JsonStoreKeys.TELEMETRY_CELLS_SNAPSHOT)
            _snapshotFlow.value = null
        }
    }

    private suspend fun loadFromStore(): TelemetryCellsSnapshot? {
        val raw = configRepository.getJson(JsonStoreKeys.TELEMETRY_CELLS_SNAPSHOT) ?: return null
        return runCatching {
            json.decodeFromString(TelemetryCellsSnapshot.serializer(), raw)
        }.getOrElse {
            Timber.e(it, "TelemetryCellsRepository: loadFromStore")
            null
        }
    }

    private suspend fun saveToStore(snapshot: TelemetryCellsSnapshot) {
        configRepository.setJson(
            JsonStoreKeys.TELEMETRY_CELLS_SNAPSHOT,
            json.encodeToString(TelemetryCellsSnapshot.serializer(), snapshot),
        )
    }
}
