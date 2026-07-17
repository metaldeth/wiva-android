package com.wiva.android.data.payment.aqsi

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.domain.model.AqsiConfig
import com.wiva.android.domain.model.AqsiPaymentResult
import com.wiva.android.services.payment.CardPaymentEventLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AqsiRepositoryImplTest {

    @Test
    fun initiatePayment_whenClientApproves_returnsSuccessApproved() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    payResult =
                        kotlin.Result.success(AqsiPaymentResult.Approved),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            val result = repo.initiatePayment(100)
            assertTrue(result.isSuccess)
            assertEquals(AqsiPaymentResult.Approved, result.getOrNull())
            assertEquals(AqsiDiagnosticOutcome.APPROVED, holder.getSnapshot()?.outcome)
        }

    @Test
    fun initiatePayment_whenClientDeclined_returnsDeclined() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    payResult =
                        kotlin.Result.success(AqsiPaymentResult.Declined(publicCode = "051")),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            val result = repo.initiatePayment(199)
            assertTrue(result.isSuccess)
            assertEquals(AqsiPaymentResult.Declined("051"), result.getOrNull())
            assertEquals(AqsiDiagnosticOutcome.DECLINED, holder.getSnapshot()?.outcome)
        }

    @Test
    fun initiatePayment_whenTransportTimeout_returnsFailure() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    payResult =
                        kotlin.Result.failure(AqsiTransportException("timeout")),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            val result = repo.initiatePayment(50)
            assertTrue(result.isFailure)
            assertEquals(AqsiDiagnosticOutcome.ERROR, holder.getSnapshot()?.outcome)
        }

    @Test
    fun initiatePayment_whenSocketBroken_returnsFailure() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    payResult =
                        kotlin.Result.failure(AqsiTransportException("io_error")),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            assertTrue(repo.initiatePayment(1).isFailure)
            assertEquals(AqsiDiagnosticOperationKind.PAYMENT, holder.getSnapshot()?.operationKind)
        }

    @Test
    fun testTcpConnection_unreachablePropagatesFailure() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    tcpResult =
                        kotlin.Result.failure(AqsiTransportException("io_error")),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            assertTrue(repo.testTcpConnection().isFailure)
            assertEquals(AqsiDiagnosticOperationKind.TCP_TEST, holder.getSnapshot()?.operationKind)
        }

    @Test
    fun testTcpConnection_success_updatesHolderOk() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client = RecordingArcusClient(tcpResult = kotlin.Result.success(Unit))
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            assertTrue(repo.testTcpConnection().isSuccess)
            assertEquals(AqsiDiagnosticOutcome.SUCCESS, holder.getSnapshot()?.outcome)
        }

    @Test
    fun cancelPayment_callsClient() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client = RecordingArcusClient()
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            repo.cancelPayment()
            assertEquals(1, client.interruptCalls)
            assertEquals(1, client.cancelCalls)
            assertEquals(AqsiDiagnosticOperationKind.CANCEL, holder.getSnapshot()?.operationKind)
        }

    @Test
    fun initiatePayment_whenAmountNegative_returnsFailureWithoutCallingClient() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client = RecordingArcusClient()
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            val result = repo.initiatePayment(-1)
            assertTrue(result.isFailure)
            val ex = result.exceptionOrNull()
            assertTrue(ex is AqsiTransportException)
            assertEquals("invalid_amount_kopecks", ex?.message)
            assertEquals(AqsiDiagnosticOutcome.ERROR, holder.getSnapshot()?.outcome)
            assertEquals(0, client.initiateCalls)
        }

    @Test
    fun initiatePayment_whenClientCancelled_recordsCancelledOutcome() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    payResult =
                        kotlin.Result.success(AqsiPaymentResult.Cancelled),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            val result = repo.initiatePayment(200)
            assertTrue(result.isSuccess)
            assertEquals(AqsiPaymentResult.Cancelled, result.getOrNull())
            assertEquals(AqsiDiagnosticOutcome.CANCELLED, holder.getSnapshot()?.outcome)
        }

    @Test
    fun initiatePayment_whenParserReturnsErDeclined_recordsDeclined() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val client =
                RecordingArcusClient(
                    payResult =
                        kotlin.Result.success(
                            AqsiPaymentResult.Declined(JpayPaymentOutcomeParser.DECLINED_PUBLIC_CODE_ER),
                        ),
                )
            val repo =
                AqsiRepositoryImpl(
                    configRepository = fakeCfg(validHost()),
                    arcus2 = client,
                    lastOperationSnapshotHolder = holder,
                    paymentEventLogger = CardPaymentEventLogger(),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            val result = repo.initiatePayment(50)
            assertTrue(result.isSuccess)
            assertEquals(
                AqsiPaymentResult.Declined(JpayPaymentOutcomeParser.DECLINED_PUBLIC_CODE_ER),
                result.getOrNull(),
            )
            assertEquals(AqsiDiagnosticOutcome.DECLINED, holder.getSnapshot()?.outcome)
        }

    private fun validHost(): AqsiConfig =
        AqsiConfig(host = "192.168.1.10", port = 16107, timeoutMs = 15000L)

    private fun fakeCfg(cfg: AqsiConfig): ConfigRepository =
            object : ConfigRepository {
            private val json =
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            private val store =
                mutableMapOf(
                    JsonStoreKeys.AQSI_SETTINGS to json.encodeToString(AqsiConfig.serializer(), cfg),
                )

            override suspend fun get(key: String): String? = store[key]

            override suspend fun set(key: String, value: String) {
                store[key] = value
            }

            override suspend fun delete(key: String) {
                store.remove(key)
            }

            override suspend fun getJson(key: String): String? = store[key]

            override suspend fun setJson(key: String, jsonStr: String) {
                store[key] = jsonStr
            }
        }

    private open class RecordingArcusClient(
        var tcpResult: kotlin.Result<Unit> = kotlin.Result.success(Unit),
        var payResult: kotlin.Result<AqsiPaymentResult> =
            kotlin.Result.success(AqsiPaymentResult.Approved),
        var cancelResult: kotlin.Result<Unit> = kotlin.Result.success(Unit),
    ) : Arcus2TerminalClient {
        var cancelCalls = 0
        var interruptCalls = 0
        var initiateCalls = 0

        override fun interruptCurrentTcpSession() {
            interruptCalls++
        }

        override fun testTcpChannel(host: String, port: Int, timeoutMs: Long): kotlin.Result<Unit> =
            tcpResult

        override fun initiatePurchase(host: String, port: Int, timeoutMs: Long, amountKopecks: Int): kotlin.Result<AqsiPaymentResult> {
            initiateCalls++
            return payResult
        }

        override fun cancelPurchase(host: String, port: Int, timeoutMs: Long): kotlin.Result<Unit> {
            cancelCalls++
            return cancelResult
        }
    }
}
