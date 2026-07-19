package com.wiva.android.data.remote.telemetry.mvp

import android.os.Build
import com.wiva.android.BuildConfig
import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.local.security.MachineSecretStore
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
 * Координатор simple-telemetry MVP: REG register + JWT WS + legacy enroll fallback.
 */
@Singleton
class SimpleTelemetryCoordinator
@Inject
constructor(
    private val apiClient: MvpTelemetryApiClient,
    private val wsManager: MvpTelemetryWebSocketManager,
    private val configRepository: ConfigRepository,
    private val machineSecretStore: MachineSecretStore,
    private val jwtCache: MachineJwtCache,
    @AppIoScope private val appScope: CoroutineScope,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    val connectionState: StateFlow<ConnectionState> = wsManager.connectionState

    private val lifecycleMutex = Mutex()
    private var connectJob: Job? = null

    private suspend fun isUserPaused(): Boolean =
        configRepository.get(JsonStoreKeys.TELEMETRY_PAUSED_BY_USER) == "true"

    private suspend fun setUserPaused(paused: Boolean) {
        configRepository.set(
            JsonStoreKeys.TELEMETRY_PAUSED_BY_USER,
            if (paused) "true" else "false",
        )
    }

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

    @Volatile
    private var cachedUseMvpProtocol: Boolean = false

    suspend fun loadMachineRegistration(): MachineRegistration {
        val raw =
            configRepository.getJson(com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION)
                ?: return MachineRegistration.migrateLegacy(MachineRegistration())
        return runCatching { json.decodeFromString<MachineRegistration>(raw) }
            .getOrDefault(MachineRegistration())
            .let { MachineRegistration.migrateLegacy(it) }
    }

    suspend fun saveMachineRegistration(reg: MachineRegistration) {
        val sanitized =
            reg.copy(
                machineCredential = "",
                machineKey = "",
            )
        configRepository.setJson(
            com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION,
            json.encodeToString(MachineRegistration.serializer(), sanitized),
        )
    }

    private suspend fun persistRegistrationMetadata(reg: MachineRegistration) {
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
        if (updated.authScheme != MachineRegistration.AUTH_SCHEME_STABLE_SECRET &&
            updated.machineCredential.isBlank() &&
            !machineSecretStore.hasSecret(updated.serialNumber)
        ) {
            updated =
                updated.copy(
                    machineCredential =
                        if (updated.machineKey.startsWith("mch_")) {
                            updated.machineKey
                        } else {
                            MachineCredentialGenerator.generate()
                        },
                    authScheme = MachineRegistration.AUTH_SCHEME_LEGACY_CREDENTIAL,
                )
        }
        if (updated != reg) {
            configRepository.setJson(
                com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION,
                json.encodeToString(MachineRegistration.serializer(), updated),
            )
        }
        return updated
    }

    suspend fun hasStableSecret(serialNumber: String): Boolean =
        machineSecretStore.hasSecret(SerialNumberUtils.normalize(serialNumber))

    suspend fun registerMachine(
        registrationKey: String,
        serialNumber: String,
    ): Result<Unit> {
        val normalizedKey = RegistrationKeyUtils.normalize(registrationKey)
        RegistrationKeyUtils.validationMessage(normalizedKey)?.let { msg ->
            return Result.failure(IllegalArgumentException(msg))
        }
        val normalizedSerial = SerialNumberUtils.normalize(serialNumber)
        SerialNumberUtils.validationMessage(normalizedSerial)?.let { msg ->
            return Result.failure(IllegalArgumentException(msg))
        }
        val config = loadTelemetryConfig()
        val reg = ensureIdentity(loadMachineRegistration())
        val previousSerial = SerialNumberUtils.normalize(reg.serialNumber)
        val request =
            RegisterRequestDto(
                registrationKey = normalizedKey,
                serialNumber = normalizedSerial,
                installationId = reg.installationId,
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
            .register(config.apiUrl, request)
            .map { response ->
                val enrolledSerial =
                    SerialNumberUtils.normalize(
                        response.serialNumber.ifBlank { normalizedSerial },
                    )
                if (previousSerial.isNotBlank() && previousSerial != enrolledSerial) {
                    machineSecretStore.clearSecret(previousSerial)
                }
                machineSecretStore.saveSecret(enrolledSerial, response.machineSecret)
                val wsUrl =
                    MvpTelemetryUrlResolver.resolveWsUrl(
                        apiBaseUrl = config.apiUrl,
                        enrolledWsUrl = response.wsUrl,
                        configuredWsUrl = config.wsUrl,
                    )
                val updated =
                    reg.copy(
                        serialNumber = enrolledSerial,
                        machineId = response.machineId.ifBlank { response.id },
                        installationId = response.installationId.ifBlank { reg.installationId },
                        wsProtocolUrl = wsUrl,
                        tokenEndpoint = response.tokenEndpoint,
                        regKey = normalizedKey,
                        authScheme = MachineRegistration.AUTH_SCHEME_STABLE_SECRET,
                        machineCredential = "",
                        machineKey = "",
                        isRegistered = true,
                        enrolled = true,
                        reservationToken = "",
                        reservationExpiresAt = "",
                    )
                jwtCache.invalidate()
                persistRegistrationMetadata(updated)
                Timber.i("SimpleTelemetry: registered serial=${updated.serialNumber}, auth=stable_secret")
            }
    }

    /**
     * Поле serial в UI отличается от сохранённого — отключаем WS, сбрасываем enroll и секрет старого serial.
     * @return true, если состояние регистрации было изменено.
     */
    suspend fun applyUiSerialChange(uiSerial: String): Boolean {
        val reg = loadMachineRegistration()
        val normUi = SerialNumberUtils.normalize(uiSerial)
        val normPersisted = SerialNumberUtils.normalize(reg.serialNumber)
        if (normUi == normPersisted) {
            return false
        }
        disconnectInternal(persistPause = false)
        jwtCache.invalidate()
        if (normPersisted.isNotBlank() && machineSecretStore.hasSecret(normPersisted)) {
            machineSecretStore.clearSecret(normPersisted)
        }
        val serialInStore = normUi.ifBlank { normPersisted }
        val reset =
            MachineRegistration.resetAfterSerialChange(
                reg = reg,
                serialInStore = serialInStore,
                newInstallationId = UUID.randomUUID().toString(),
            )
        persistRegistrationMetadata(reset)
        Timber.i(
            "SimpleTelemetry: serial field changed ($normPersisted → $normUi), enrollment reset",
        )
        return true
    }

    suspend fun connectWithGuard(uiSerial: String): Result<Unit> {
        SerialNumberUtils.validationMessage(uiSerial)?.let { msg ->
            return Result.failure(IllegalArgumentException(msg))
        }
        val normUi = SerialNumberUtils.normalize(uiSerial)
        val reg = ensureIdentity(loadMachineRegistration())
        val normPersisted = SerialNumberUtils.normalize(reg.serialNumber)
        if (normUi != normPersisted) {
            return Result.failure(
                IllegalStateException(
                    "Серийный номер в поле ($normUi) не совпадает с сохранённым ($normPersisted). " +
                        "Сначала выполните регистрацию.",
                ),
            )
        }
        if (!MachineRegistration.isEnrolled(reg)) {
            return Result.failure(
                IllegalStateException("Машина не зарегистрирована. Нажмите «Регистрация»."),
            )
        }
        if (!hasAuthMaterial(reg)) {
            return Result.failure(
                IllegalStateException(
                    "Нет секрета для серийного номера $normUi. Выполните регистрацию.",
                ),
            )
        }
        setUserPaused(false)
        scheduleConnect("явное подключение")
        return Result.success(Unit)
    }

    private suspend fun hasAuthMaterial(reg: MachineRegistration): Boolean {
        val serial = SerialNumberUtils.normalize(reg.serialNumber)
        if (machineSecretStore.hasSecret(serial)) {
            return true
        }
        val legacy =
            reg.machineCredential.ifBlank {
                if (reg.machineKey.startsWith("mch_")) reg.machineKey else ""
            }
        return legacy.isNotBlank()
    }

    /** Автоподключение и connect после успешной REG — только по сохранённому serial. */
    fun connectAuto() {
        appScope.launch {
            if (isUserPaused()) {
                Timber.d("SimpleTelemetry: connectAuto пропущен — пауза пользователя")
                return@launch
            }
            setUserPaused(false)
            scheduleConnect("автоподключение")
        }
    }

    fun connect() {
        connectAuto()
    }

    /** Legacy enroll с X-Enrollment-Key — только для обратной совместимости. */
    suspend fun reserveFreeSerial(): Result<String> {
        val config = loadTelemetryConfig()
        val reg = ensureIdentity(loadMachineRegistration())
        return apiClient
            .reserveSerial(config.apiUrl, reg.installationId)
            .map { response ->
                val normalized = SerialNumberUtils.normalize(response.serialNumber)
                configRepository.setJson(
                    com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION,
                    json.encodeToString(
                        MachineRegistration.serializer(),
                        reg.copy(
                            serialNumber = normalized,
                            reservationToken = response.reservationToken,
                            reservationExpiresAt = response.expiresAt,
                        ),
                    ),
                )
                normalized
            }
    }

    /** Legacy enroll с X-Enrollment-Key — только для обратной совместимости. */
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
                        authScheme = MachineRegistration.AUTH_SCHEME_LEGACY_CREDENTIAL,
                        wsProtocolUrl = wsUrl,
                        isRegistered = true,
                        enrolled = true,
                        reservationToken = "",
                        reservationExpiresAt = "",
                    )
                configRepository.setJson(
                    com.wiva.android.data.local.db.JsonStoreKeys.MACHINE_REGISTRATION,
                    json.encodeToString(MachineRegistration.serializer(), reg),
                )
                Timber.i("SimpleTelemetry: legacy enrolled serial=${reg.serialNumber}")
            }
    }

    fun disconnect() {
        appScope.launch {
            disconnectInternal(persistPause = true)
        }
    }

    /** Отключить WS без флага «пауза пользователя» (смена serial в поле). */
    fun disconnectWithoutPause() {
        appScope.launch {
            disconnectInternal(persistPause = false)
        }
    }

    private suspend fun disconnectInternal(persistPause: Boolean) {
        if (persistPause) {
            setUserPaused(true)
        }
        jwtCache.invalidate()
        connectJob?.cancel()
        connectJob = null
        wsManager.disconnect()
    }

    fun reconnect() {
        appScope.launch {
            if (isUserPaused()) {
                Timber.d("SimpleTelemetry: reconnect пропущен — пауза пользователя")
                return@launch
            }
            setUserPaused(false)
            scheduleConnect("reconnect")
        }
    }

    private fun scheduleConnect(reason: String) {
        connectJob?.cancel()
        wsManager.disconnect()
        connectJob =
            appScope.launch {
                lifecycleMutex.withLock {
                    if (isUserPaused()) return@launch
                    connectInternal(reason)
                }
            }
    }

    private suspend fun connectInternal(reason: String) {
        if (isUserPaused()) return
        val config = loadTelemetryConfig()
        val reg = ensureIdentity(loadMachineRegistration())
        if (!MachineRegistration.isEnrolled(reg)) {
            Timber.w("SimpleTelemetry: registration required ($reason)")
            return
        }
        val wsUrl =
            MvpTelemetryUrlResolver.resolveWsUrl(
                apiBaseUrl = config.apiUrl,
                enrolledWsUrl = reg.wsProtocolUrl,
                configuredWsUrl = config.wsUrl,
            )
        Timber.i("SimpleTelemetry: WS ($reason) url=$wsUrl")
        wsManager.connect(
            wsUrl = wsUrl,
            tokenProvider = { resolveWsBearerToken(config, reg) },
            temperatureProvider = { null },
            onAuthFailure = { jwtCache.invalidate() },
        )
    }

    private suspend fun resolveWsBearerToken(
        config: TelemetryConfig,
        reg: MachineRegistration,
    ): String? {
        val serial = reg.serialNumber
        val stableSecret = machineSecretStore.getSecret(serial)
        if (!stableSecret.isNullOrBlank()) {
            return jwtCache
                .getAccessToken(
                    serialNumber = serial,
                    machineSecret = stableSecret,
                ) {
                    apiClient.fetchToken(
                        baseUrl = config.apiUrl,
                        tokenEndpoint = reg.tokenEndpoint,
                        requestBody =
                            TokenRequestDto(
                                serialNumber = serial,
                                machineSecret = stableSecret,
                            ),
                    )
                }.getOrElse { error ->
                    Timber.w(error, "SimpleTelemetry: token fetch failed")
                    if (error is TokenAuthException) {
                        wsManager.reportAuthFailure("Ошибка авторизации JWT")
                    }
                    return null
                }
        }
        val legacyCredential =
            reg.machineCredential.ifBlank {
                if (reg.machineKey.startsWith("mch_")) reg.machineKey else ""
            }
        if (legacyCredential.isNotBlank()) {
            return legacyCredential
        }
        Timber.w("SimpleTelemetry: no auth material for WS")
        return null
    }

    suspend fun isMvpProtocolEnabled(): Boolean = cachedUseMvpProtocol

    internal suspend fun obtainWsBearerTokenForTests(
        config: TelemetryConfig,
        reg: MachineRegistration,
    ): String? = resolveWsBearerToken(config, reg)
}
