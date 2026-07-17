package com.wiva.android.data.remote.telemetry.mvp

import android.os.Build
import com.wiva.android.BuildConfig
import com.wiva.android.data.remote.telemetry.ConnectionState
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.di.AppIoScope
import com.wiva.android.domain.model.MachineRegistration
import com.wiva.android.domain.model.TelemetryConfig
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Координатор simple-telemetry MVP: reserve/enroll REST + WS lifecycle.
 * Legacy Keycloak/topic-протокол не используется.
 */
@Singleton
class SimpleTelemetryCoordinator
@Inject
constructor(
    private val apiClient: MvpTelemetryApiClient,
    private val wsManager: MvpTelemetryWebSocketManager,
    private val configRepository: ConfigRepository,
    @AppIoScope private val appScope: CoroutineScope,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    val connectionState: StateFlow<ConnectionState> = wsManager.connectionState

    private val lifecycleMutex = Mutex()
    private var connectJob: Job? = null

    @Volatile
    private var pausedByUser = false

    @Volatile
    private var cachedUseMvpProtocol: Boolean = false

    private val _mvpProtocolEnabled = MutableStateFlow(false)
    val mvpProtocolEnabled: StateFlow<Boolean> = _mvpProtocolEnabled.asStateFlow()

    suspend fun loadTelemetryConfig(): TelemetryConfig {
        val config = readTelemetryConfigFromStore()
        updateMvpProtocolCache(config.useMvpProtocol)
        return config
    }

    suspend fun saveTelemetryConfig(config: TelemetryConfig) {
        val migrated = TelemetryConfig.migrateLegacy(config)
        configRepository.setJson(
            com.wiva.android.data.local.db.JsonStoreKeys.TELEMETRY_CONFIG,
            json.encodeToString(TelemetryConfig.serializer(), migrated),
        )
        updateMvpProtocolCache(migrated.useMvpProtocol)
    }

    private suspend fun readTelemetryConfigFromStore(): TelemetryConfig {
        val raw = configRepository.getJson(com.wiva.android.data.local.db.JsonStoreKeys.TELEMETRY_CONFIG)
            ?: return TelemetryConfig.migrateLegacy(TelemetryConfig())
        return runCatching { json.decodeFromString<TelemetryConfig>(raw) }
            .getOrDefault(TelemetryConfig())
            .let { TelemetryConfig.migrateLegacy(it) }
    }

    private fun updateMvpProtocolCache(enabled: Boolean) {
        cachedUseMvpProtocol = enabled
        _mvpProtocolEnabled.value = enabled
    }

    suspend fun loadMachineRegistration(): MachineRegistration {
        val raw =
            configRepository.getJson(com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION)
                ?: return MachineRegistration.migrateLegacy(MachineRegistration())
        return runCatching { json.decodeFromString<MachineRegistration>(raw) }
            .getOrDefault(MachineRegistration())
            .let { MachineRegistration.migrateLegacy(it) }
    }

    suspend fun saveMachineRegistration(reg: MachineRegistration) {
        configRepository.setJson(
            com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION,
            json.encodeToString(MachineRegistration.serializer(), reg),
        )
    }

    suspend fun ensureIdentity(reg: MachineRegistration): MachineRegistration {
        var updated = reg
        if (updated.installationId.isBlank()) {
            updated = updated.copy(installationId = UUID.randomUUID().toString())
        }
        if (updated.machineCredential.isBlank()) {
            updated =
                updated.copy(
                    machineCredential =
                        if (updated.machineKey.startsWith("mch_")) {
                            updated.machineKey
                        } else {
                            MachineCredentialGenerator.generate()
                        },
                )
        }
        if (updated != reg) saveMachineRegistration(updated)
        return updated
    }

    suspend fun reserveFreeSerial(): Result<String> {
        val config = loadTelemetryConfig()
        val reg = ensureIdentity(loadMachineRegistration())
        return apiClient
            .reserveSerial(config.apiUrl, reg.installationId)
            .map { response ->
                val normalized = SerialNumberUtils.normalize(response.serialNumber)
                saveMachineRegistration(
                    reg.copy(
                        serialNumber = normalized,
                        reservationToken = response.reservationToken,
                        reservationExpiresAt = response.expiresAt,
                    ),
                )
                normalized
            }
    }

    suspend fun enrollMachine(serialNumber: String, rebind: Boolean): Result<Unit> {
        val normalized = SerialNumberUtils.normalize(serialNumber)
        SerialNumberUtils.validationMessage(normalized)?.let { msg ->
            return Result.failure(IllegalArgumentException(msg))
        }
        val config = loadTelemetryConfig()
        var reg = ensureIdentity(loadMachineRegistration())
        val reservationToken = reg.reservationToken.takeIf { it.isNotBlank() }
        val request =
            EnrollRequestDto(
                installationId = reg.installationId,
                serialNumber = normalized,
                reservationToken = reservationToken,
                credential = reg.machineCredential,
                rebind = rebind,
                device =
                    EnrollDeviceDto(
                        manufacturer = Build.MANUFACTURER.orEmpty(),
                        model = Build.MODEL.orEmpty(),
                        androidVersion = Build.VERSION.RELEASE.orEmpty(),
                    ),
                app =
                    EnrollAppDto(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                    ),
            )
        return apiClient
            .enroll(config.apiUrl, request)
            .map { response ->
                val wsUrl =
                    MvpTelemetryUrlResolver.resolveWsUrl(
                        apiBaseUrl = config.apiUrl,
                        enrolledWsUrl = response.wsProtocolUrl ?: response.wsUrl,
                        configuredWsUrl = config.wsUrl,
                    )
                reg =
                    reg.copy(
                        serialNumber = response.serialNumber.ifBlank { normalized },
                        machineId = response.machineId.ifBlank { reg.machineId },
                        machineKey = reg.machineCredential,
                        machineCredential = reg.machineCredential,
                        wsProtocolUrl = wsUrl,
                        isRegistered = true,
                        enrolled = true,
                        reservationToken = "",
                        reservationExpiresAt = "",
                    )
                saveMachineRegistration(reg)
                Timber.i("SimpleTelemetry: enrolled serial=${reg.serialNumber}, ws=$wsUrl")
            }
    }

    fun connect() {
        pausedByUser = false
        scheduleConnect("явное подключение")
    }

    fun disconnect() {
        pausedByUser = true
        connectJob?.cancel()
        connectJob = null
        wsManager.disconnect()
    }

    fun reconnect() {
        pausedByUser = false
        scheduleConnect("reconnect")
    }

    private fun scheduleConnect(reason: String) {
        connectJob?.cancel()
        wsManager.disconnect()
        connectJob =
            appScope.launch {
                lifecycleMutex.withLock {
                    if (pausedByUser) return@launch
                    connectInternal(reason)
                }
            }
    }

    private suspend fun connectInternal(reason: String) {
        if (pausedByUser) return
        val config = loadTelemetryConfig()
        val reg = ensureIdentity(loadMachineRegistration())
        if (!MachineRegistration.isEnrolled(reg)) {
            Timber.w("SimpleTelemetry: enroll required ($reason)")
            return
        }
        val credential = reg.machineCredential.ifBlank { reg.machineKey }
        if (credential.isBlank()) {
            Timber.w("SimpleTelemetry: credential missing ($reason)")
            return
        }
        val wsUrl =
            MvpTelemetryUrlResolver.resolveWsUrl(
                apiBaseUrl = config.apiUrl,
                enrolledWsUrl = reg.wsProtocolUrl,
                configuredWsUrl = config.wsUrl,
            )
        Timber.i("SimpleTelemetry: WS ($reason) url=$wsUrl")
        wsManager.connect(wsUrl, credential) { null }
    }

    suspend fun isMvpProtocolEnabled(): Boolean = cachedUseMvpProtocol
}
