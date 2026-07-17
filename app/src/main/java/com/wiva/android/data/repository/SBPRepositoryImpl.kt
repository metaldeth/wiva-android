package com.wiva.android.data.repository

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.remote.sbp.PAYMASTER_QPAY_BASE_URL
import com.wiva.android.data.remote.sbp.PaymasterQPayHelper
import com.wiva.android.data.remote.sbp.PaymasterXmlParser
import com.wiva.android.domain.model.SBPLink
import com.wiva.android.domain.model.SBPSettings
import com.wiva.android.domain.model.SBPStatus
import com.wiva.android.domain.repository.SBPRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class SBPRepositoryImpl
@Inject
constructor(
    private val okHttpClient: OkHttpClient,
    private val configRepository: ConfigRepository,
) : SBPRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override suspend fun getSBPLink(amountKopecks: Int): Result<SBPLink> =
        withContext(Dispatchers.IO) {
            val result =
                runCatching {
                    val settings = getSettings()
                    require(settings.spotId.isNotBlank()) { "Не задан Spot ID Paymaster" }
                    require(settings.key.isNotBlank()) { "Не задан секретный ключ Paymaster" }

                    val orderId = PaymasterQPayHelper.generateReqn()
                    val reqn = PaymasterQPayHelper.generateReqn()
                    val sign = PaymasterQPayHelper.calculateSign(settings.spotId, orderId, "sbp", reqn, settings.key)
                    val amountStr = PaymasterQPayHelper.amountRublesString(amountKopecks)
                    val xmlBody =
                        PaymasterQPayHelper.createGenerateQrXml(
                            spotId = settings.spotId,
                            orderId = orderId,
                            amountRubles = amountStr,
                            reqn = reqn,
                            sign = sign,
                            timeoutSeconds = settings.timeoutInSeconds,
                        )

                    Timber.d("SBP generate_qr: orderId=$orderId reqn=$reqn amount=$amountStr")
                    Timber.d("SBP generate_qr XML:\n$xmlBody")

                    val url = "${PAYMASTER_QPAY_BASE_URL.trimEnd('/')}/generate_qr_code"
                    val request =
                        Request.Builder()
                            .url(url)
                            .post(xmlBody.toRequestBody("application/xml".toMediaType()))
                            .header("Accept", "application/xml")
                            .header("Accept-Language", "en")
                            .build()

                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string().orEmpty()
                    Timber.d("SBP generate_qr HTTP=%s body.len=%d", response.code, body.length)
                    Timber.d("SBP generate_qr body:\n%s", body.take(4000))
                    if (!response.isSuccessful) {
                        error("Paymaster HTTP ${response.code}: ${body.take(500)}")
                    }

                    val parsed = PaymasterXmlParser.parseQPayGenerateQr(body)
                    val paymentUrlFallback = PaymasterXmlParser.parsePaymentUrl(body)
                    Timber.d(
                        "SBP parsed: retval=%s retdesc=%s qr=%s paymentUrl=%s",
                        parsed.retval,
                        parsed.retdesc,
                        parsed.qr,
                        paymentUrlFallback,
                    )
                    if (parsed.retval != 0) {
                        error(parsed.retdesc.ifBlank { "Paymaster retval=${parsed.retval}" })
                    }
                    val qrUrl =
                        parsed.qr?.takeIf { it.isNotBlank() }
                            ?: paymentUrlFallback?.takeIf { it.isNotBlank() }
                            ?: error("Нет ссылки QR в ответе Paymaster (<qr> или <PaymentUrl>)")

                    val updated =
                        settings.copy(
                            lastOrderId = orderId,
                            lastRequestNumber = reqn,
                            lastSign = sign,
                        )
                    updateSettings(updated)

                    SBPLink(orderId = orderId, url = qrUrl, qrData = qrUrl)
                }
            result.exceptionOrNull()?.let { Timber.e(it, "SBP getSBPLink failed") }
            result
        }

    override suspend fun getSBPLinkStatus(orderId: String): Result<SBPStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = getSettings()
                if (orderId.isNotBlank() && settings.lastOrderId.isNotBlank() && orderId != settings.lastOrderId) {
                    error("Несовпадение orderId сессии оплаты")
                }
                require(settings.spotId.isNotBlank() && settings.key.isNotBlank()) { "СБП не настроен" }
                require(settings.lastRequestNumber.isNotBlank() && settings.lastOrderId.isNotBlank()) {
                    "Нет активного заказа СБП"
                }

                val reqn = settings.lastRequestNumber
                val sign = PaymasterQPayHelper.calculateSignGetOutInvoices(settings.spotId, reqn, settings.key)
                val xmlBody =
                    PaymasterQPayHelper.createGetOutInvoicesXml(
                        reqn = reqn,
                        sign = sign,
                        posid = settings.spotId,
                        orderId = settings.lastOrderId,
                    )

                val url = "${PAYMASTER_QPAY_BASE_URL.trimEnd('/')}/get_outinvoices"
                val request =
                    Request.Builder()
                        .url(url)
                        .post(xmlBody.toRequestBody("application/xml".toMediaType()))
                        .header("Accept", "application/xml")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Paymaster HTTP ${response.code}: ${body.take(500)}")
                }

                val state = PaymasterXmlParser.parseQPayOutInvoiceState(body)
                PaymasterXmlParser.parseQPayStatusToSbpStatus(state)
            }
        }

    override suspend fun cancelSBPLink(orderId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = getSettings()
                if (settings.lastOrderId.isBlank() || settings.lastRequestNumber.isBlank()) {
                    return@runCatching
                }
                if (orderId.isNotBlank() && orderId != settings.lastOrderId) {
                    error("Несовпадение orderId при отмене")
                }

                val sign =
                    PaymasterQPayHelper.calculateSignCancelOrder(
                        settings.spotId,
                        settings.lastOrderId,
                        settings.lastRequestNumber,
                        settings.key,
                    )
                val xmlBody =
                    PaymasterQPayHelper.createCancelOrderXml(
                        reqn = settings.lastRequestNumber,
                        sign = sign,
                        posid = settings.spotId,
                        orderId = settings.lastOrderId,
                    )

                val url = "${PAYMASTER_QPAY_BASE_URL.trimEnd('/')}/cancel_order"
                val request =
                    Request.Builder()
                        .url(url)
                        .post(xmlBody.toRequestBody("application/xml".toMediaType()))
                        .header("Accept", "application/xml")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Paymaster cancel HTTP ${response.code}: ${body.take(500)}")
                }
                val retval = PaymasterXmlParser.parseQPayCancelRetval(body)
                if (retval != 0) {
                    error("Отмена заказа Paymaster retval=$retval")
                }
            }
        }

    override suspend fun getSettings(): SBPSettings {
        val raw = configRepository.getJson(JsonStoreKeys.SBP_SETTINGS) ?: return SBPSettings()
        return runCatching { json.decodeFromString<SBPSettings>(raw) }.getOrDefault(SBPSettings())
    }

    override suspend fun updateSettings(settings: SBPSettings) {
        configRepository.setJson(JsonStoreKeys.SBP_SETTINGS, json.encodeToString(settings))
    }
}
