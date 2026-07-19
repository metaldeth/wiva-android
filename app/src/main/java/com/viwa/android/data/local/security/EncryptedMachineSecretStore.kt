package com.viwa.android.data.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EncryptedMachineSecretStore
@Inject
constructor(
    @ApplicationContext context: Context,
) : MachineSecretStore {
    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    override suspend fun getSecret(serialNumber: String): String? =
        withContext(Dispatchers.IO) {
            val key = prefKey(serialNumber) ?: return@withContext null
            prefs.getString(key, null)?.takeIf { it.isNotBlank() }
        }

    override suspend fun saveSecret(serialNumber: String, secret: String) =
        withContext(Dispatchers.IO) {
            val key = prefKey(serialNumber) ?: return@withContext
            prefs.edit().putString(key, secret.trim()).apply()
        }

    override suspend fun clearSecret(serialNumber: String) =
        withContext(Dispatchers.IO) {
            val key = prefKey(serialNumber) ?: return@withContext
            prefs.edit().remove(key).apply()
        }

    override suspend fun hasSecret(serialNumber: String): Boolean =
        withContext(Dispatchers.IO) {
            val key = prefKey(serialNumber) ?: return@withContext false
            !prefs.getString(key, null).isNullOrBlank()
        }

    private fun prefKey(serialNumber: String): String? {
        val normalized = serialNumber.trim().uppercase()
        if (normalized.isBlank()) return null
        return "$KEY_PREFIX$normalized"
    }

    private companion object {
        const val PREFS_FILE = "wiva_machine_secrets"
        const val KEY_PREFIX = "secret_"
    }
}
