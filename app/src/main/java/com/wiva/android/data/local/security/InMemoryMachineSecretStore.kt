package com.wiva.android.data.local.security

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** In-memory store for JVM unit tests (Robolectric-friendly fake). */
class InMemoryMachineSecretStore : MachineSecretStore {
    private val secrets = ConcurrentHashMap<String, String>()
    private val mutex = Mutex()

    override suspend fun getSecret(serialNumber: String): String? =
        mutex.withLock {
            secrets[serialNumber.trim().uppercase()]
        }

    override suspend fun saveSecret(serialNumber: String, secret: String) {
        mutex.withLock {
            secrets[serialNumber.trim().uppercase()] = secret.trim()
        }
    }

    override suspend fun clearSecret(serialNumber: String) {
        mutex.withLock {
            secrets.remove(serialNumber.trim().uppercase())
        }
    }

    override suspend fun hasSecret(serialNumber: String): Boolean =
        mutex.withLock {
            !secrets[serialNumber.trim().uppercase()].isNullOrBlank()
        }
}
