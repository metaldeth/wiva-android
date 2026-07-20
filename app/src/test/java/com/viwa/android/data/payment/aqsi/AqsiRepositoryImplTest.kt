package com.viwa.android.data.payment.aqsi

import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.services.payment.CardPaymentEventLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AqsiRepositoryImplTest {

    private fun fakeConfig(): ConfigRepository =
        object : ConfigRepository {
            override suspend fun get(key: String): String? = null

            override suspend fun set(key: String, value: String) = Unit

            override suspend fun delete(key: String) = Unit

            override suspend fun getJson(key: String): String? = null

            override suspend fun setJson(key: String, jsonStr: String) = Unit
        }

    private fun mockManager(): AqsiUsbPaymentManager {
        val m = mockk<AqsiUsbPaymentManager>(relaxed = true)
        every { m.exchangeLogFlow } returns MutableStateFlow<List<String>>(emptyList())
        every { m.terminalStatusFlow } returns MutableStateFlow("")
        return m
    }

    private fun repo(
        manager: AqsiUsbPaymentManager,
        holder: AqsiLastOperationSnapshotHolder = AqsiLastOperationSnapshotHolder(),
    ): AqsiRepositoryImpl =
        AqsiRepositoryImpl(
            configRepository = fakeConfig(),
            usbPaymentManager = manager,
            lastOperationSnapshotHolder = holder,
            paymentEventLogger = CardPaymentEventLogger(),
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun initiatePayment_whenUsbApproves_returnsApproved() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            val manager = mockManager()
            coEvery { manager.pay(100) } returns UsbPaymentResult.Success("ok", 100)
            val result = repo(manager, holder).initiatePayment(100)
            assertTrue(result.isSuccess)
            assertEquals(AqsiPaymentResult.Approved, result.getOrNull())
            assertEquals(AqsiDiagnosticOutcome.APPROVED, holder.getSnapshot()?.outcome)
        }

    @Test
    fun initiatePayment_whenUsbDeclined_returnsDeclined() =
        runBlocking {
            val manager = mockManager()
            coEvery { manager.pay(199) } returns UsbPaymentResult.Failure("AQSI_DECLINED_051", "declined")
            val result = repo(manager).initiatePayment(199)
            assertTrue(result.getOrNull() is AqsiPaymentResult.Declined)
        }

    @Test
    fun initiatePayment_whenAmountNegative_returnsFailureWithoutCallingUsb() =
        runBlocking {
            val manager = mockManager()
            val result = repo(manager).initiatePayment(-1)
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { manager.pay(any()) }
        }

    @Test
    fun cancelPayment_invokesUsbCancel() =
        runBlocking {
            val manager = mockManager()
            repo(manager).cancelPayment()
            coVerify(exactly = 1) { manager.cancel() }
        }
}
