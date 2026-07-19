package com.viwa.android.data.local.security

/** Encrypted persistence for per-machine stable secrets (`machineSecret`). Never log values. */
interface MachineSecretStore {
    suspend fun getSecret(serialNumber: String): String?

    suspend fun saveSecret(serialNumber: String, secret: String)

    suspend fun clearSecret(serialNumber: String)

    suspend fun hasSecret(serialNumber: String): Boolean
}
