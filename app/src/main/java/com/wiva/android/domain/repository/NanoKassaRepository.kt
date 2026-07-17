package com.wiva.android.domain.repository

import com.wiva.android.domain.model.FiscalReceipt
import com.wiva.android.domain.model.NanoKassaSettings
import com.wiva.android.domain.model.PaymentMethod
import com.wiva.android.domain.model.ReceiptItem

interface NanoKassaRepository {
    suspend fun sendFiscalReceipt(
        amountKopecks: Int,
        items: List<ReceiptItem>,
        paymentMethod: PaymentMethod,
        isTest: Boolean = false,
    ): Result<FiscalReceipt>

 /**
 * Тестовый чек (`isTest = true`) — без боевой фискализации; обновляет [NanoKassaSettings.lastIntegrationVerifyOk].
 */
    suspend fun verifyIntegration(): Result<Unit>

 /** Поля заполнены и последняя проверка интеграции успешна. */
    suspend fun isNanoKassaOperational(): Boolean

 /**
 * Достаточно данных для попытки боевого чека (kassaId, token, kkt, адрес, место).
 * Не требует [NanoKassaSettings.lastIntegrationVerifyOk] — иначе гость не увидит QR после оплаты,
 * если verify на старте не прошёл или ещё не выполняли «Сохранить» в сервисе.
 */
    suspend fun hasNanoFiscalConfig(): Boolean

    suspend fun getSettings(): NanoKassaSettings

    suspend fun updateSettings(settings: NanoKassaSettings)
}
