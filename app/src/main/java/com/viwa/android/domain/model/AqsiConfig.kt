package com.viwa.android.domain.model

import kotlinx.serialization.Serializable

/** Типичный TCP-порт Arcus2 / JPAY для Pill T7100 (см. ТЗ по aQsi). */
const val AQSI_DEFAULT_TCP_PORT = 16107

/** Дефолтный таймаут сетевых операций к ридеру, мс. */
const val AQSI_DEFAULT_TIMEOUT_MS = 15_000L

/**
 * Параметры TCP-подключения к ридеру aQsi. Персистится в JsonStore под ключом
 * [com.viwa.android.data.local.db.JsonStoreKeys.AQSI_SETTINGS].
 */
@Serializable
data class AqsiConfig(
    val host: String = "",
    val port: Int = AQSI_DEFAULT_TCP_PORT,
    val timeoutMs: Long = AQSI_DEFAULT_TIMEOUT_MS,
)
