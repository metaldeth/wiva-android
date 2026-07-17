package com.wiva.android.data.remote.telemetry.mvp

import java.security.SecureRandom

/** Генерация machine credential с префиксом `mch_`. */
object MachineCredentialGenerator {
    private const val PREFIX = "mch_"
    private const val TOKEN_BYTES = 32
    private val secureRandom = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        val encoded = bytes.joinToString("") { b -> "%02x".format(b) }
        return PREFIX + encoded
    }

    fun isValid(credential: String): Boolean =
        credential.startsWith(PREFIX) && credential.length > PREFIX.length + 16
}
