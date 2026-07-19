package com.viwa.android.domain.repository

import com.viwa.android.domain.model.AqsiConfig
import com.viwa.android.domain.model.AqsiPaymentResult

/**
 * Доступ к настройке и операциям ридера aQsi. Конфиг читается/пишется в JsonStore —
 * ключ [com.viwa.android.data.local.db.JsonStoreKeys.AQSI_SETTINGS].
 *
 * Реализация (TCP/JPAY) — последующие задачи пайплайна.
 */
interface AqsiRepository {

    suspend fun loadConfig(): AqsiConfig

    suspend fun saveConfig(config: AqsiConfig)

 /**
 * Проверка канала без финансовой транзакции (TCP connect в пределах [AqsiConfig.timeoutMs],
 * закрытие сокета; при необходимости — минимальный handshake, не списание).
 */
    suspend fun testTcpConnection(): Result<Unit>

    suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult>

    suspend fun cancelPayment(): Result<Unit>
}
