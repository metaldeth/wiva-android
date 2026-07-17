package com.wiva.android.data.remote.telemetry.mvp

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.network.NetworkTrafficLogger
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.domain.model.MachineRegistration
import com.wiva.android.domain.model.TelemetryConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleTelemetryCoordinatorLifecycleTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `saveTelemetryConfig updates cached mvp protocol flag`() = runTest {
        // given
        val configRepository = mockk<ConfigRepository>(relaxed = true)
        coEvery { configRepository.getJson(JsonStoreKeys.TELEMETRY_CONFIG) } returns null
        val coordinator = createCoordinator(configRepository)
        // when
        coordinator.saveTelemetryConfig(TelemetryConfig(useMvpProtocol = true))
        // then
        assertTrue(coordinator.isMvpProtocolEnabled())
        assertTrue(coordinator.mvpProtocolEnabled.value)
    }

    @Test
    fun `parallel connect calls invoke ws connect once`() = runTest {
        // given
        val configRepository = InMemoryConfigRepository()
        val enrolled =
            MachineRegistration(
                serialNumber = "WIVA-000001",
                machineCredential = "mch_test",
                machineKey = "mch_test",
                wsProtocolUrl = "ws://127.0.0.1/ws",
                isRegistered = true,
                enrolled = true,
                installationId = "inst-1",
            )
        configRepository.setJson(
            JsonStoreKeys.MACHINE_REGISTRATION,
            json.encodeToString(MachineRegistration.serializer(), enrolled),
        )
        configRepository.setJson(
            JsonStoreKeys.TELEMETRY_CONFIG,
            json.encodeToString(TelemetryConfig.serializer(), TelemetryConfig(useMvpProtocol = true)),
        )
        var connectCalls = 0
        val wsManager = mockk<MvpTelemetryWebSocketManager>(relaxed = true)
        every { wsManager.connectionState } returns
            MutableStateFlow(com.wiva.android.data.remote.telemetry.ConnectionState.Disconnected())
        every { wsManager.connect(any(), any(), any()) } answers { connectCalls++ }
        every { wsManager.disconnect() } just runs
        val coordinator =
            SimpleTelemetryCoordinator(
                apiClient = mockk(relaxed = true),
                wsManager = wsManager,
                configRepository = configRepository,
                appScope = this,
            )
        // when
        coordinator.connect()
        coordinator.connect()
        coordinator.reconnect()
        advanceUntilIdle()
        // then
        assertTrue("expected single ws connect, got $connectCalls", connectCalls == 1)
    }

    @Test
    fun `loadTelemetryConfig refreshes cache when toggled off`() = runTest {
        // given
        val configRepository = InMemoryConfigRepository()
        configRepository.setJson(
            JsonStoreKeys.TELEMETRY_CONFIG,
            json.encodeToString(TelemetryConfig.serializer(), TelemetryConfig(useMvpProtocol = false)),
        )
        val coordinator = createCoordinator(configRepository)
        // when
        coordinator.loadTelemetryConfig()
        // then
        assertFalse(coordinator.isMvpProtocolEnabled())
    }

    private fun createCoordinator(configRepository: ConfigRepository): SimpleTelemetryCoordinator =
        SimpleTelemetryCoordinator(
            apiClient = mockk(relaxed = true),
            wsManager =
                MvpTelemetryWebSocketManager(
                    appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()),
                    networkTrafficLogger = mockk<NetworkTrafficLogger>(relaxed = true),
                ),
            configRepository = configRepository,
            appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()),
        )

    private class InMemoryConfigRepository : ConfigRepository {
        private val store = mutableMapOf<String, String>()

        override suspend fun get(key: String): String? = store[key]

        override suspend fun set(key: String, value: String) {
            store[key] = value
        }

        override suspend fun delete(key: String) {
            store.remove(key)
        }

        override suspend fun getJson(key: String): String? = store[key]

        override suspend fun setJson(key: String, json: String) {
            store[key] = json
        }
    }
}
