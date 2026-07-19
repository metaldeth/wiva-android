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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val TAG_AQSI_REPO = "AqsiRepo"

/**
 * Реализация [AqsiRepository]: JsonStore + IO + вызовы [Arcus2TerminalClient].
 * DI — task-04 ([com.viwa.android.di.AqsiModule]).
 */
class AqsiRepositoryImpl(
    private val configRepository: ConfigRepository,
    private val arcus2: Arcus2TerminalClient,
    private val lastOperationSnapshotHolder: AqsiLastOperationSnapshotHolder,
    private val paymentEventLogger: CardPaymentEventLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AqsiRepository {

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

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
            val cfg = loadStoredConfig()
            if (cfg.host.isBlank()) {
                val err = Result.failure<Unit>(IllegalArgumentException("no_host"))
                recordTcpTest(cfg, err)
                Timber.tag(TAG_AQSI_REPO).w("tcp_test skipped: empty host")
                return@withContext err
            }
            val result = arcus2.testTcpChannel(cfg.host, cfg.port, cfg.timeoutMs)
            recordTcpTest(cfg, result)
            if (result.isSuccess) {
                paymentEventLogger.info(
                    "Новый считыватель",
                    "TCP-соединение OK",
                    lane = CardPaymentLogLane.FromTerminal,
                )
                Timber.tag(TAG_AQSI_REPO).i("tcp_test ok")
            } else {
                val ex = result.exceptionOrNull()
                paymentEventLogger.error(
                    "Новый считыватель",
                    "TCP-соединение не прошло",
                    ex?.javaClass?.simpleName ?: "unknown",
                    lane = CardPaymentLogLane.FromTerminal,
                )
                if (ex != null) {
                    Timber.tag(TAG_AQSI_REPO).w(
                        "tcp_test failed err=%s",
                        ex.javaClass.simpleName,
                    )
                } else {
                    Timber.tag(TAG_AQSI_REPO).w("tcp_test failed")
                }
            }
            result
        }

    override suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult> =
        withContext(ioDispatcher) {
            val cfg = loadStoredConfig()
            if (amountKopecks < 0) {
                val err =
                    Result.failure<AqsiPaymentResult>(
                        AqsiTransportException("invalid_amount_kopecks"),
                    )
                recordPayment(err)
                Timber.tag(TAG_AQSI_REPO).w("payment skipped: invalid amount")
                return@withContext err
            }
            if (cfg.host.isBlank()) {
                val err =
                    Result.failure<AqsiPaymentResult>(
                        IllegalArgumentException(
                            "В настройках «Новый считыватель» не указан адрес (host). " +
                                "Задайте IP в сервисном меню или включите mock aQsi для теста.",
                        ),
                    )
                recordPayment(err)
                Timber.tag(TAG_AQSI_REPO).w("payment skipped: empty host")
                return@withContext err
            }
            coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause is CancellationException) {
                    runCatching { arcus2.interruptCurrentTcpSession() }
                }
            }
            val result = arcus2.initiatePurchase(cfg.host, cfg.port, cfg.timeoutMs, amountKopecks)
            recordPayment(result)
            logPaymentAggregate(result)
            result
        }

    override suspend fun cancelPayment(): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { arcus2.interruptCurrentTcpSession() }.onFailure { ex ->
                Timber.tag(TAG_AQSI_REPO).w(ex, "interruptTcpSession skipped")
            }
            val cfg = loadStoredConfig()
            if (cfg.host.isBlank()) {
                val err = Result.failure<Unit>(IllegalArgumentException("no_host"))
                recordCancel(err)
                return@withContext err
            }
            coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause is CancellationException) {
                    runCatching { arcus2.interruptCurrentTcpSession() }
                }
            }
            val result = arcus2.cancelPurchase(cfg.host, cfg.port, cfg.timeoutMs)
            recordCancel(result)
            result.exceptionOrNull()?.let { ex ->
                Timber.tag(TAG_AQSI_REPO).w(ex, "cancel failed")
            }
            result
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

    private fun recordTcpTest(cfg: AqsiConfig, result: Result<Unit>) {
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
                operationKind = AqsiDiagnosticOperationKind.TCP_TEST,
                outcome = outcome,
                detailCode = detail,
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
            is Arcus2ProtocolException -> "protocol"
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
