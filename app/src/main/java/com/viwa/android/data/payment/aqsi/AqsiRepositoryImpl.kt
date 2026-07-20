package com.viwa.android.data.payment.aqsi

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.model.AqsiConfig
import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.services.payment.CardPaymentEventLogger
import com.viwa.android.services.payment.CardPaymentLogLane
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val TAG_AQSI_REPO = "AqsiRepo"

class AqsiRepositoryImpl(
    private val configRepository: ConfigRepository,
    private val usbPaymentManager: AqsiUsbPaymentManager,
    private val lastOperationSnapshotHolder: AqsiLastOperationSnapshotHolder,
    private val paymentEventLogger: CardPaymentEventLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AqsiRepository {

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    val exchangeLogFlow: StateFlow<List<String>> = usbPaymentManager.exchangeLogFlow

    val terminalStatusFlow: StateFlow<String> = usbPaymentManager.terminalStatusFlow

    override suspend fun loadConfig(): AqsiConfig =
        withContext(ioDispatcher) {
            loadStoredConfig()
        }

    override suspend fun saveConfig(config: AqsiConfig) =
        withContext(ioDispatcher) {
            val encoded = json.encodeToString(AqsiConfig.serializer(), config)
            configRepository.setJson(JsonStoreKeys.AQSI_SETTINGS, encoded)
            Timber.tag(TAG_AQSI_REPO).d("config saved hostSet=${config.host.isNotBlank()}")
        }

    override suspend fun testTcpConnection(): Result<Unit> =
        withContext(ioDispatcher) {
            val result =
                runCatching {
                    val payment = usbPaymentManager.testPayment()
                    val domain = mapUsbToDomain(payment)
                    if (domain !== AqsiPaymentResult.Approved) {
                        throw IllegalStateException("probe_not_approved")
                    }
                }
            recordProbe(result.map { AqsiPaymentResult.Approved })
            if (result.isSuccess) {
                paymentEventLogger.info(
                    "aQsi USB",
                    "USB/Arcus2 probe OK",
                    lane = CardPaymentLogLane.FromTerminal,
                )
                Result.success(Unit)
            } else {
                val err = result.exceptionOrNull() ?: IllegalStateException("probe_failed")
                paymentEventLogger.error(
                    "aQsi USB",
                    "USB/Arcus2 probe failed",
                    err.message ?: "probe_failed",
                    lane = CardPaymentLogLane.FromTerminal,
                )
                Result.failure(err)
            }
        }

    override suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult> =
        withContext(ioDispatcher) {
            if (amountKopecks < 0) {
                val err = Result.failure<AqsiPaymentResult>(AqsiTransportException("invalid_amount_kopecks"))
                recordPayment(err)
                return@withContext err
            }
            val result = runCatching { usbPaymentManager.pay(amountKopecks) }.map { mapUsbToDomain(it) }
            recordPayment(result)
            logPaymentAggregate(result)
            result
        }

    override suspend fun cancelPayment(): Result<Unit> =
        withContext(ioDispatcher) {
            usbPaymentManager.cancel()
            recordCancel(Result.success(Unit))
            Result.success(Unit)
        }

    private suspend fun loadStoredConfig(): AqsiConfig {
        val raw = configRepository.getJson(JsonStoreKeys.AQSI_SETTINGS) ?: return AqsiConfig()
        if (raw.isBlank()) return AqsiConfig()
        return runCatching {
            json.decodeFromString(AqsiConfig.serializer(), raw)
        }.getOrElse {
            Timber.tag(TAG_AQSI_REPO).w(it, "config decode failed, defaults")
            AqsiConfig()
        }
    }

    private fun mapUsbToDomain(result: UsbPaymentResult): AqsiPaymentResult =
        when (result) {
            is UsbPaymentResult.Success -> AqsiPaymentResult.Approved
            is UsbPaymentResult.Failure ->
                if (result.errorCode.startsWith("AQSI_DECLINED")) {
                    AqsiPaymentResult.Declined(result.errorCode.removePrefix("AQSI_DECLINED_"))
                } else {
                    AqsiPaymentResult.Error(result.message.take(256))
                }
            UsbPaymentResult.Cancelled -> AqsiPaymentResult.Cancelled
            UsbPaymentResult.Timeout -> AqsiPaymentResult.Error("timeout")
        }

    private fun recordProbe(result: Result<AqsiPaymentResult>) {
        val outcome =
            when {
                result.isSuccess && result.getOrNull() === AqsiPaymentResult.Approved -> AqsiDiagnosticOutcome.SUCCESS
                else -> AqsiDiagnosticOutcome.ERROR
            }
        lastOperationSnapshotHolder.update(
            AqsiLastOperationSummary(
                timestampMillis = System.currentTimeMillis(),
                operationKind = AqsiDiagnosticOperationKind.TCP_TEST,
                outcome = outcome,
                detailCode = summaryDetail(result.exceptionOrNull()),
            ),
        )
    }

    private fun recordPayment(result: Result<AqsiPaymentResult>) {
        val payment = result.getOrNull()
        val outcome: AqsiDiagnosticOutcome
        val detail: String
        when {
            result.isFailure -> {
                outcome = AqsiDiagnosticOutcome.ERROR
                detail = summaryDetail(result.exceptionOrNull())
            }
            payment == null -> {
                outcome = AqsiDiagnosticOutcome.ERROR
                detail = "unknown"
            }
            payment === AqsiPaymentResult.Approved -> {
                outcome = AqsiDiagnosticOutcome.APPROVED
                detail = ""
            }
            payment is AqsiPaymentResult.Declined -> {
                outcome = AqsiDiagnosticOutcome.DECLINED
                detail = payment.publicCode.take(16)
            }
            payment is AqsiPaymentResult.Error -> {
                outcome = AqsiDiagnosticOutcome.ERROR
                detail = payment.safeMessage.take(48)
            }
            payment === AqsiPaymentResult.Cancelled -> {
                outcome = AqsiDiagnosticOutcome.CANCELLED
                detail = ""
            }
            else -> {
                outcome = AqsiDiagnosticOutcome.ERROR
                detail = "unknown"
            }
        }
        lastOperationSnapshotHolder.update(
            AqsiLastOperationSummary(
                timestampMillis = System.currentTimeMillis(),
                operationKind = AqsiDiagnosticOperationKind.PAYMENT,
                outcome = outcome,
                detailCode = detail,
            ),
        )
    }

    private fun recordCancel(result: Result<Unit>) {
        val outcome =
            when {
                result.isSuccess -> AqsiDiagnosticOutcome.SUCCESS
                else -> AqsiDiagnosticOutcome.ERROR
            }
        val detail =
            when {
                result.isSuccess -> ""
                else -> summaryDetail(result.exceptionOrNull())
            }
        lastOperationSnapshotHolder.update(
            AqsiLastOperationSummary(
                timestampMillis = System.currentTimeMillis(),
                operationKind = AqsiDiagnosticOperationKind.CANCEL,
                outcome = outcome,
                detailCode = detail,
            ),
        )
    }

    private fun summaryDetail(t: Throwable?): String =
        when (t) {
            is IllegalArgumentException -> t.message?.take(32) ?: "argument"
            is AqsiTransportException -> t.message?.take(32) ?: "transport"
            else -> t?.javaClass?.simpleName?.take(24) ?: "error"
        }

    private fun logPaymentAggregate(result: Result<AqsiPaymentResult>) {
        val label =
            when {
                result.isFailure -> "failure"
                result.getOrNull() is AqsiPaymentResult.Approved -> "approved"
                result.getOrNull() is AqsiPaymentResult.Declined -> "declined"
                result.getOrNull() is AqsiPaymentResult.Error -> "error"
                result.getOrNull() === AqsiPaymentResult.Cancelled -> "cancelled"
                else -> "unknown"
            }
        Timber.tag(TAG_AQSI_REPO).i("payment aggregate: $label")
    }
}
