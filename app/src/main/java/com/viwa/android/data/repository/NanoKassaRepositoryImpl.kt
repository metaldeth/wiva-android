package com.viwa.android.data.repository

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.remote.nanokassa.NanoKassaEncryptionHelper
import com.viwa.android.data.remote.nanokassa.NanoKassaEncryptedRequest
import com.viwa.android.data.remote.nanokassa.NanoKassaItogDto
import com.viwa.android.data.remote.nanokassa.NanoKassaJson
import com.viwa.android.data.remote.nanokassa.NanoKassaOplataDto
import com.viwa.android.data.remote.nanokassa.NanoKassaProductDto
import com.viwa.android.data.remote.nanokassa.NanoKassaRequestBody
import com.viwa.android.data.remote.nanokassa.NanoKassaServerResponse
import com.viwa.android.domain.model.FiscalReceipt
import com.viwa.android.domain.model.FiscalStatus
import com.viwa.android.domain.model.MachineRegistration
import com.viwa.android.domain.model.NanoKassaSettings
import com.viwa.android.domain.model.PaymentMethod
import com.viwa.android.domain.model.ReceiptItem
import com.viwa.android.domain.repository.NanoKassaRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val NANO_KASSA_URL = "http://q.nanokassa.ru/srv/igd.php"
private const val CHECK_PAGE_BASE = "https://check.nanokassa.com/check.php"

private const val VERIFY_ITEM_NAME = "Проверка интеграции"

class NanoKassaRepositoryImpl
@Inject
constructor(
    private val okHttpClient: OkHttpClient,
    private val configRepository: ConfigRepository,
) : NanoKassaRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()

    override suspend fun sendFiscalReceipt(
        amountKopecks: Int,
        items: List<ReceiptItem>,
        paymentMethod: PaymentMethod,
        isTest: Boolean,
    ): Result<FiscalReceipt> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = getSettings()
                requireSettingsPresent(settings)

                val machineSerial = loadMachineSerial()
                val isElectronic = paymentMethod == PaymentMethod.SBP || paymentMethod == PaymentMethod.CARD

                val products =
                    items.map { item ->
                        val priceKop = item.price
                        val summaKop = priceKop * item.quantity
                        NanoKassaProductDto(
                            nameTavar = item.name,
                            pricePieceBezSkidki = priceKop,
                            kolvo = item.quantity,
                            pricePiece = priceKop,
                            summa = summaKop,
                        )
                    }

                val itogCheka = products.sumOf { it.summa }
                val orderLabel = "Заказ ${SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())}"

                val requestBody =
                    NanoKassaRequestBody(
                        kassaid = settings.kassaId,
                        kassatoken = settings.kassaToken,
                        nameZakaz = orderLabel,
                        checkVendAddress = settings.address,
                        checkVendMesto = settings.place,
                        checkVendNumAvtovat = machineSerial,
                        productsArr = products,
                        oplataArr =
                            NanoKassaOplataDto(
                                moneyNal = if (isElectronic) 0 else amountKopecks,
                                moneyElectro = if (isElectronic) amountKopecks else 0,
                            ),
                        itogArr = NanoKassaItogDto(itogCheka = itogCheka),
                    )

                val serverResponse = postEncryptedReceipt(settings, requestBody, isTest).getOrThrow()

                val checkPageUrl = buildCheckPageUrl(settings.kkt, serverResponse.nuid, serverResponse.qnuid)
                Timber.d("[NanoKassa] checkPageUrl: $checkPageUrl")

                FiscalReceipt(
                    receiptId = serverResponse.nuid,
                    kktId = settings.kkt,
                    amount = amountKopecks,
                    status = FiscalStatus.SUCCESS,
                    nuid = serverResponse.nuid,
                    qnuid = serverResponse.qnuid,
                    checkPageUrl = checkPageUrl,
                )
            }
        }

    override suspend fun verifyIntegration(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = getSettings()
                val now = System.currentTimeMillis()
                if (!hasRequiredFieldsForApi(settings)) {
                    persistVerifyResult(settings, ok = false, at = now)
                    error("NanoKassa: не заполнены kassaId, kassaToken, kkt, адрес или место")
                }
                val machineSerial = loadMachineSerial()
                val amountKopecks = 1
                val products =
                    listOf(
                        NanoKassaProductDto(
                            nameTavar = VERIFY_ITEM_NAME,
                            pricePieceBezSkidki = 1,
                            kolvo = 1,
                            pricePiece = 1,
                            summa = 1,
                        ),
                    )
                val orderLabel = "Verify ${SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())}"
                val requestBody =
                    NanoKassaRequestBody(
                        kassaid = settings.kassaId,
                        kassatoken = settings.kassaToken,
                        nameZakaz = orderLabel,
                        checkVendAddress = settings.address,
                        checkVendMesto = settings.place,
                        checkVendNumAvtovat = machineSerial,
                        productsArr = products,
                        oplataArr =
                            NanoKassaOplataDto(
                                moneyNal = 0,
                                moneyElectro = amountKopecks,
                            ),
                        itogArr = NanoKassaItogDto(itogCheka = amountKopecks),
                    )
                try {
                    postEncryptedReceipt(settings, requestBody, isTest = true).getOrElse { throw it }
                    persistVerifyResult(settings, ok = true, at = now)
                } catch (e: Throwable) {
                    persistVerifyResult(settings, ok = false, at = now)
                    throw e
                }
            }
        }

    override suspend fun isNanoKassaOperational(): Boolean {
        val s = getSettings()
        return hasRequiredFieldsForApi(s) && s.lastIntegrationVerifyOk
    }

    override suspend fun hasNanoFiscalConfig(): Boolean = hasRequiredFieldsForApi(getSettings())

    private suspend fun persistVerifyResult(settings: NanoKassaSettings, ok: Boolean, at: Long) {
        updateSettings(
            settings.copy(
                lastIntegrationVerifyOk = ok,
                lastVerifyAtEpochMs = at,
            ),
        )
    }

    private fun hasRequiredFieldsForApi(settings: NanoKassaSettings): Boolean =
        settings.kassaId.isNotBlank() &&
            settings.kassaToken.isNotBlank() &&
            settings.kkt.isNotBlank() &&
            settings.address.isNotBlank() &&
            settings.place.isNotBlank()

    private fun requireSettingsPresent(settings: NanoKassaSettings) {
        require(settings.kassaId.isNotBlank()) { "NanoKassa не настроена: kassaId пустой" }
        require(settings.kassaToken.isNotBlank()) { "NanoKassa не настроена: kassaToken пустой" }
        require(settings.kkt.isNotBlank()) { "NanoKassa не настроена: kkt пустой" }
    }

    private suspend fun postEncryptedReceipt(
        settings: NanoKassaSettings,
        requestBody: NanoKassaRequestBody,
        isTest: Boolean,
    ): Result<NanoKassaServerResponse> =
        runCatching {
            val requestJson = NanoKassaJson.encodeToString(NanoKassaRequestBody.serializer(), requestBody)
            Timber.d("[NanoKassa] requestJson (isTest=$isTest): $requestJson")

            val encryptedBody =
                NanoKassaEncryptionHelper.buildEncryptedBody(
                    requestJson = requestJson,
                    kassaId = settings.kassaId,
                    kassaToken = settings.kassaToken,
                    isTest = isTest,
                )

            val encryptedJson = NanoKassaJson.encodeToString(NanoKassaEncryptedRequest.serializer(), encryptedBody)

            val httpRequest =
                Request.Builder()
                    .url(NANO_KASSA_URL)
                    .post(encryptedJson.toRequestBody(contentType))
                    .header("Content-Type", "application/json")
                    .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: error("Пустой ответ от NanoKassa")

            Timber.d("[NanoKassa] response ${response.code}: $responseBody")

            if (!response.isSuccessful) {
                error("NanoKassa HTTP ${response.code}: $responseBody")
            }

            val serverResponse = NanoKassaJson.decodeFromString(NanoKassaServerResponse.serializer(), responseBody)

            if (serverResponse.nuid.isBlank() || serverResponse.qnuid.isBlank()) {
                val errMsg = serverResponse.error.ifBlank { "NanoKassa: nuid/qnuid пусты (${serverResponse.code})" }
                error(errMsg)
            }
            serverResponse
        }

    override suspend fun getSettings(): NanoKassaSettings {
        val jsonStr = configRepository.getJson(JsonStoreKeys.NANOKASSA_SETTINGS)
            ?: return NanoKassaSettings()
        return runCatching {
            json.decodeFromString<NanoKassaSettings>(jsonStr)
        }.getOrDefault(NanoKassaSettings())
    }

    override suspend fun updateSettings(settings: NanoKassaSettings) {
        configRepository.setJson(
            JsonStoreKeys.NANOKASSA_SETTINGS,
            json.encodeToString(settings),
        )
    }

    private suspend fun loadMachineSerial(): String {
        val jsonStr = configRepository.getJson(JsonStoreKeys.MACHINE_REGISTRATION) ?: return ""
        return runCatching {
            json.decodeFromString<MachineRegistration>(jsonStr).serialNumber
        }.getOrDefault("")
    }

    companion object {
        fun buildCheckPageUrl(kkt: String, nuid: String, qnuid: String): String =
            "$CHECK_PAGE_BASE?zav_kkt=$kkt&nuid=$nuid&qnuid=$qnuid"
    }
}
