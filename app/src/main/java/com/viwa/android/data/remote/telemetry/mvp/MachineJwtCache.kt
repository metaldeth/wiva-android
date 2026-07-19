package com.viwa.android.data.remote.telemetry.mvp

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory JWT cache for machine WebSocket auth.
 * Never persisted or logged — tokens live only in process memory.
 */
@Singleton
class MachineJwtCache
@Inject
constructor(
    private val clock: EpochMillisClock,
) {
    private data class CacheEntry(
        val accessToken: String,
        val expiresAtEpochMs: Long,
        val refreshSkewSeconds: Int,
        val serialNumber: String,
        val machineSecret: String,
    )

    private val mutex = Mutex()

    @Volatile
    private var entry: CacheEntry? = null

    fun invalidate() {
        entry = null
    }

    suspend fun getAccessToken(
        serialNumber: String,
        machineSecret: String,
        fetch: suspend () -> Result<TokenResponseDto>,
    ): Result<String> =
        mutex.withLock {
            val now = clock.epochMillis()
            val cached = entryForIdentity(serialNumber, machineSecret, now)
            if (cached != null && !shouldRefresh(cached, now)) {
                return Result.success(cached.accessToken)
            }

            val staleEntry = entry
            fetch().fold(
                onSuccess = { response ->
                    if (response.accessToken.isBlank()) {
                        entry = null
                        Result.failure(TokenAuthException("Пустой accessToken"))
                    } else {
                        val skew = refreshSkewSeconds(response.expiresIn)
                        entry =
                            CacheEntry(
                                accessToken = response.accessToken,
                                expiresAtEpochMs = now + response.expiresIn.coerceAtLeast(1) * 1000L,
                                refreshSkewSeconds = skew,
                                serialNumber = serialNumber,
                                machineSecret = machineSecret,
                            )
                        Result.success(response.accessToken)
                    }
                },
                onFailure = { error ->
                    if (error is TokenAuthException) {
                        entry = null
                        Result.failure(error)
                    } else if (
                        staleEntry != null &&
                        staleEntry.serialNumber == serialNumber &&
                        staleEntry.machineSecret == machineSecret &&
                        isStillValid(staleEntry, now)
                    ) {
                        Result.success(staleEntry.accessToken)
                    } else {
                        Result.failure(error)
                    }
                },
            )
        }

    private fun entryForIdentity(
        serialNumber: String,
        machineSecret: String,
        now: Long,
    ): CacheEntry? {
        val current = entry ?: return null
        if (current.serialNumber != serialNumber || current.machineSecret != machineSecret) {
            entry = null
            return null
        }
        if (!isStillValid(current, now)) {
            entry = null
            return null
        }
        return current
    }

    private fun shouldRefresh(
        cached: CacheEntry,
        now: Long,
    ): Boolean {
        val refreshAt = cached.expiresAtEpochMs - cached.refreshSkewSeconds * 1000L
        return now >= refreshAt
    }

    private fun isStillValid(
        cached: CacheEntry,
        now: Long,
    ): Boolean = now < cached.expiresAtEpochMs

    companion object {
        const val MAX_REFRESH_SKEW_SECONDS = 60
        const val MIN_REFRESH_SKEW_SECONDS = 1

        fun refreshSkewSeconds(expiresInSeconds: Int): Int =
            min(
                MAX_REFRESH_SKEW_SECONDS,
                max(MIN_REFRESH_SKEW_SECONDS, expiresInSeconds / 10),
            )
    }
}
