package com.viwa.android.services.telemetry

import com.viwa.android.data.remote.telemetry.ConnectionState
import com.viwa.android.data.remote.telemetry.mvp.RegistrationKeyUtils
import com.viwa.android.data.remote.telemetry.mvp.SimpleTelemetryCoordinator
import com.viwa.android.di.AppIoScope
import com.viwa.android.domain.model.MachineRegistration
import com.viwa.android.domain.model.TelemetryConfig
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.data.local.db.JsonStoreKeys
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Фасад телеметрии: MVP WS через [SimpleTelemetryCoordinator].
 * Legacy Shaker topic-WS и Keycloak удалены.
 */
@Singleton
class ViwaTelemetryService
@Inject
constructor(
    private val configRepository: ConfigRepository,
    private val mvpCoordinator: SimpleTelemetryCoordinator,
    @AppIoScope private val scope: CoroutineScope,
) {
    private companion object {
        const val SUBSCRIPTION_SALE_TIMEOUT_MS = 60_000L
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }

    val connectionState: StateFlow<ConnectionState> = mvpCoordinator.connectionState

    private val subscriptionSaleTimers = mutableMapOf<String, Job>()

    private val _subscribeInfo = MutableStateFlow<SubscribeInformationState?>(null)
    val subscribeInfo: StateFlow<SubscribeInformationState?> = _subscribeInfo

    private val _subscriptionLevels = MutableStateFlow<List<SubscriptionLevelItem>?>(null)
    val subscriptionLevels: StateFlow<List<SubscriptionLevelItem>?> = _subscriptionLevels

    private val _loyaltyCardClientScans =
        MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val loyaltyCardClientScans: SharedFlow<String> = _loyaltyCardClientScans.asSharedFlow()

    private val _invalidLoyaltyCardScans =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val invalidLoyaltyCardScans: SharedFlow<Unit> = _invalidLoyaltyCardScans.asSharedFlow()

    @Volatile
    private var telemetryPausedByUser: Boolean = false

    private var scheduledAutoConnect: Job? = null

    data class AuthCodeResult(
        val success: Boolean,
        val message: String,
    )

    init {
        scope.launch {
            telemetryPausedByUser =
                configRepository.get(JsonStoreKeys.TELEMETRY_PAUSED_BY_USER) == "true"
            loadTelemetryConfig()
            scheduledAutoConnect =
                launch {
                    delay(3_000)
                    scheduledAutoConnect = null
                    startTelemetryIfRegistered("холодный старт")
                }
        }
    }

    suspend fun loadTelemetryConfig(): TelemetryConfig = mvpCoordinator.loadTelemetryConfig()

    suspend fun saveTelemetryConfig(config: TelemetryConfig) = mvpCoordinator.saveTelemetryConfig(config)

    suspend fun loadMachineRegistration(): MachineRegistration = mvpCoordinator.loadMachineRegistration()

    suspend fun saveMachineRegistration(reg: MachineRegistration) = mvpCoordinator.saveMachineRegistration(reg)

    suspend fun registerMachine(
        regKey: String,
        serialNumber: String,
        rebind: Boolean = false,
    ): Result<Unit> {
        val normalizedKey = RegistrationKeyUtils.normalize(regKey)
        return if (RegistrationKeyUtils.isValid(normalizedKey)) {
            mvpCoordinator.registerMachine(normalizedKey, serialNumber)
        } else {
            mvpCoordinator.enrollMachine(serialNumber, rebind)
        }
    }

    suspend fun reserveFreeSerial(): Result<String> = mvpCoordinator.reserveFreeSerial()

    suspend fun applyUiSerialChange(uiSerial: String): Boolean = mvpCoordinator.applyUiSerialChange(uiSerial)

    suspend fun connectWithGuard(uiSerial: String): Result<Unit> = mvpCoordinator.connectWithGuard(uiSerial)

    fun connect() {
        scope.launch {
            setTelemetryPausedByUser(false)
            scheduledAutoConnect?.cancel()
            scheduledAutoConnect = null
            mvpCoordinator.connectAuto()
        }
    }

    fun disconnect() {
        scheduledAutoConnect?.cancel()
        scheduledAutoConnect = null
        scope.launch {
            setTelemetryPausedByUser(true)
            mvpCoordinator.disconnect()
        }
    }

    fun reconnect() {
        scope.launch {
            setTelemetryPausedByUser(false)
            scheduledAutoConnect?.cancel()
            scheduledAutoConnect = null
            mvpCoordinator.reconnect()
        }
    }

    private suspend fun startTelemetryIfRegistered(reason: String) {
        if (telemetryPausedByUser) {
            Timber.d("ViwaTelemetry: автоподключение пропущено ($reason) — пауза по запросу пользователя")
            return
        }
        mvpCoordinator.connect()
    }

    private suspend fun setTelemetryPausedByUser(paused: Boolean) {
        telemetryPausedByUser = paused
        configRepository.set(
            JsonStoreKeys.TELEMETRY_PAUSED_BY_USER,
            if (paused) "true" else "false",
        )
    }

    /** Legacy Shaker topic — удалён; температура позже через MVP heartbeat. */
    suspend fun sendSetMachineInfo(temperature0: Int, temperature1: Int): Result<Unit> = Result.success(Unit)

    /** Legacy saleImportTopic — удалён. */
    suspend fun sendSaleImportTopic(items: List<SaleImportItem>): Result<Unit> = Result.success(Unit)

    suspend fun sendAuthCodeRequest(code: String): AuthCodeResult =
        AuthCodeResult(false, "authCodeRequestExport удалён (только MVP)")

    /** Legacy subscription WS — удалён. */
    suspend fun sendStatusSubscribeTopic(userUuid: String): Result<Unit> = Result.success(Unit)

    suspend fun sendSubscriptionLevelRequest(): Result<Unit> = Result.success(Unit)

    fun onLoyaltyCardScanned(clientUuid: String) {
        val id = clientUuid.trim()
        if (id.isEmpty()) return
        scope.launch {
            _subscriptionLevels.value = null
            _loyaltyCardClientScans.emit(id)
            Timber.d("ViwaTelemetry: loyalty scan $id — legacy subscription WS отключён")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearSubscribeUiState() {
        _subscribeInfo.value = null
        _subscriptionLevels.value = null
        _loyaltyCardClientScans.resetReplayCache()
    }

    suspend fun sendSaleSubscribeTopic(body: SaleSubscribeTopicBody): Result<Unit> = Result.success(Unit)

    fun startSubscriptionSaleTimer(
        requestUuid: String,
        machineClientId: String,
        userUuid: String,
        machineId: Int,
    ) {
        clearSubscriptionSaleTimer(requestUuid)
    }

    fun clearSubscriptionSaleTimer(requestUuid: String) {
        subscriptionSaleTimers.remove(requestUuid)?.cancel()
    }

    suspend fun sendUseSubscriptionSaleTopic(body: UseSubscriptionSaleBody): Result<Unit> = Result.success(Unit)
}
