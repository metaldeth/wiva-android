package com.wiva.android.ui.screens.service.tabs

import com.wiva.android.domain.model.AqsiConfig
import com.wiva.android.domain.model.AqsiPaymentResult
import com.wiva.android.domain.repository.AqsiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WivaAqsiSettingsViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAqsiRepository(
        var stored: AqsiConfig = AqsiConfig(host = "10.0.0.5", port = 16107, timeoutMs = 15000L),
        var tcpResult: Result<Unit> = Result.success(Unit),
    ) : AqsiRepository {
        var lastSaved: AqsiConfig? = null
        val persistenceCallOrder = mutableListOf<String>()

        override suspend fun loadConfig(): AqsiConfig = stored

        override suspend fun saveConfig(config: AqsiConfig) {
            persistenceCallOrder.add("saveConfig")
            lastSaved = config
            stored = config
        }

        override suspend fun testTcpConnection(): Result<Unit> {
            persistenceCallOrder.add("testTcpConnection")
            return tcpResult
        }

        override suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult> =
            Result.failure(IllegalStateException("not used"))

        override suspend fun cancelPayment(): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun save_rejectsBlankHost_doesNotCallSaveConfig() =
        runBlocking {
            val repo =
                FakeAqsiRepository(
                    AqsiConfig(host = "10.0.0.5", port = 16107, timeoutMs = 15000L),
                )
            val vm = WivaAqsiSettingsViewModel(repo)
            vm.setHost("   ")
            vm.save()
            assertEquals(null, repo.lastSaved)
            assertEquals(true, vm.uiState.value.bannerIsError)
            assertEquals("Укажите адрес хоста (FQDN или IP)", vm.uiState.value.banner)
        }

    @Test
    fun save_callsSaveConfigWithEnteredFields() =
        runBlocking {
            val repo = FakeAqsiRepository(AqsiConfig())
            val vm = WivaAqsiSettingsViewModel(repo)
            vm.setHost("192.168.1.10")
            vm.setPortText("9000")
            vm.setTimeoutSecText("20")
            vm.save()
            assertEquals("192.168.1.10", repo.lastSaved!!.host)
            assertEquals(9000, repo.lastSaved!!.port)
            assertEquals(20_000L, repo.lastSaved!!.timeoutMs)
        }

    @Test
    fun testTcp_onSuccess_showsOkBanner() =
        runBlocking {
            val repo = FakeAqsiRepository()
            repo.tcpResult = Result.success(Unit)
            val vm = WivaAqsiSettingsViewModel(repo)
            vm.testTcpConnection()
            assertEquals("Соединение OK", vm.uiState.value.tcpTestBanner)
            assertEquals(false, vm.uiState.value.tcpTestIsError)
        }

    @Test
    fun testTcp_savesParsedFormBeforeTestTcpConnection() =
        runBlocking {
            val repo =
                FakeAqsiRepository(
                    AqsiConfig(host = "old-host", port = 1, timeoutMs = 1000L),
                )
            val vm = WivaAqsiSettingsViewModel(repo)
            vm.setHost("192.168.1.2")
            vm.setPortText("9001")
            vm.setTimeoutSecText("25")
            vm.testTcpConnection()
            assertEquals(
                listOf("saveConfig", "testTcpConnection"),
                repo.persistenceCallOrder,
            )
            assertEquals("192.168.1.2", repo.lastSaved!!.host)
            assertEquals(9001, repo.lastSaved!!.port)
            assertEquals(25_000L, repo.lastSaved!!.timeoutMs)
        }

    @Test
    fun testTcp_rejectsBlankHost_doesNotCallRepositoryPersistence() =
        runBlocking {
            val repo =
                FakeAqsiRepository(
                    AqsiConfig(host = "10.0.0.5", port = 16107, timeoutMs = 15000L),
                )
            val vm = WivaAqsiSettingsViewModel(repo)
            vm.setHost("")
            vm.testTcpConnection()
            assertEquals(emptyList<String>(), repo.persistenceCallOrder)
            assertEquals(true, vm.uiState.value.tcpTestIsError)
            assertEquals("Укажите адрес хоста (FQDN или IP)", vm.uiState.value.tcpTestBanner)
        }

    @Test
    fun testTcp_onFailure_showsErrorWithoutCrashing() =
        runBlocking {
            val repo = FakeAqsiRepository()
            repo.tcpResult = Result.failure(RuntimeException("econn"))
            val vm = WivaAqsiSettingsViewModel(repo)
            vm.testTcpConnection()
            assertTrue(vm.uiState.value.tcpTestIsError)
            assertTrue(
                (vm.uiState.value.tcpTestBanner ?: "").contains("econn"),
            )
        }
}
