package com.wiva.android.services.telemetry

import com.wiva.android.BuildConfig
import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.location.WivaDeviceLocationReader
import com.wiva.android.data.remote.telemetry.ConnectionState
import com.wiva.android.data.remote.telemetry.mvp.SimpleTelemetryCoordinator
import com.wiva.android.data.remote.telemetry.mvp.TelemetryIsoTimestamps
import com.wiva.android.data.remote.telemetry.WivaTelemetryAuth
import com.wiva.android.data.remote.telemetry.WivaTelemetryEventBus
import com.wiva.android.data.remote.telemetry.WivaTelemetryWebSocketManager
import com.wiva.android.data.remote.telemetry.WivaWsIncomingFrame
import com.wiva.android.data.remote.telemetry.KioskDeviceLocationBody
import com.wiva.android.data.remote.telemetry.MachineRegistrationRequestDto
import com.wiva.android.data.remote.telemetry.TelemetryApiService
import com.wiva.android.di.AppIoScope
import com.wiva.android.domain.model.MachineRegistration
import com.wiva.android.domain.model.TelemetryConfig
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.data.telemetry.inventory.CellStoreMatrixBodyWire
import com.wiva.android.data.telemetry.inventory.StoredMachineConfigWire
import com.wiva.android.domain.repository.MachineInventoryRepository
import javax.inject.Named
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import timber.log.Timber

/**
 * Транспорт
 * [manager.ts] + [messageHandler.ts].
 */
@Singleton
class WivaTelemetryService
@Inject
constructor(
    private val wsManager: WivaTelemetryWebSocketManager,
    private val eventBus: WivaTelemetryEventBus,
    private val configRepository: ConfigRepository,
    private val machineInventoryRepository: MachineInventoryRepository,
    private val telemetryApi: TelemetryApiService,
    @Named("telemetryHttp")
    private val okHttpClient: OkHttpClient,
    private val deviceLocationReader: WivaDeviceLocationReader,
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

 /**
 * WS-кадры: encodeDefaults=true чтобы `type` и другие non-null дефолты попали в JSON;
 * explicitNulls=false чтобы null-поля не кодировались — JSON идентичен.
 */
    private val jsonWs =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
            explicitNulls = false
        }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var previousConnection: ConnectionState? = null
    private var pendingAuth: CompletableDeferred<AuthCodeResult>? = null
    private val subscriptionSaleTimers = mutableMapOf<String, Job>()

    private val _subscribeInfo = MutableStateFlow<SubscribeInformationState?>(null)
    val subscribeInfo: StateFlow<SubscribeInformationState?> = _subscribeInfo

    private val _subscriptionLevels = MutableStateFlow<List<SubscriptionLevelItem>?>(null)
    val subscriptionLevels: StateFlow<List<SubscriptionLevelItem>?> = _subscriptionLevels

 /**
 * Эмит при каждом скане карты подписки (после отправки WS-запросов) — для UI экрана напитков.
 * Источник скана глобальный ([LoyaltyCardScanCoordinator]),.
 */
    private val _loyaltyCardClientScans =
        MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val loyaltyCardClientScans: SharedFlow<String> = _loyaltyCardClientScans.asSharedFlow()

 /**
 * Аналог electron `SUBSCRIPTION_INVALID_CARD`: карта была считана, но телеметрия вернула пустой clientId.
 */
    private val _invalidLoyaltyCardScans =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val invalidLoyaltyCardScans: SharedFlow<Unit> = _invalidLoyaltyCardScans.asSharedFlow()

 /**
 * Пока true — не поднимать WS (кнопка «Отключить»). После перезапуска приложения сбрасывается;
 *.connect`.
 */
    @Volatile
    private var telemetryPausedByUser: Boolean = false

    @Volatile
    private var cachedUseMvpProtocol: Boolean = false

    private var scheduledAutoConnect: Job? = null

    data class AuthCodeResult(
        val success: Boolean,
        val message: String,
    )

    init {
        scope.launch {
            loadTelemetryConfig()
            launch {
                mvpCoordinator.connectionState.collect { state ->
                    if (cachedUseMvpProtocol) {
                        _connectionState.value = state
                    }
                }
            }
            launch {
                wsManager.connectionState.collect { state ->
                    if (!cachedUseMvpProtocol) {
                        _connectionState.value = state
                    }
                }
            }
            launch {
                eventBus.incoming.collect { frame -> handleIncoming(frame) }
            }
            launch {
                connectionState.collect { state ->
                    val becameConnected =
                        state is ConnectionState.Connected &&
                            previousConnection !is ConnectionState.Connected
                    previousConnection = state
                    if (becameConnected && !cachedUseMvpProtocol) {
                        scope.launch { sendInitialExchange() }
                    }
                }
            }
            scheduledAutoConnect =
                launch {
                    delay(3_000)
                    scheduledAutoConnect = null
                    startTelemetryIfRegistered("холодный старт")
                }
        }
    }

    private suspend fun readTelemetryConfigFromStore(): TelemetryConfig {
        val raw = configRepository.getJson(JsonStoreKeys.TELEMETRY_CONFIG) ?: return TelemetryConfig.migrateLegacy(TelemetryConfig())
        return runCatching { json.decodeFromString<TelemetryConfig>(raw) }.getOrDefault(TelemetryConfig()).let {
            TelemetryConfig.migrateLegacy(it)
        }
    }

    private fun updateMvpProtocolCache(useMvp: Boolean) {
        cachedUseMvpProtocol = useMvp
    }

    suspend fun loadTelemetryConfig(): TelemetryConfig {
        val config = readTelemetryConfigFromStore()
        updateMvpProtocolCache(config.useMvpProtocol)
        return config
    }

    suspend fun saveTelemetryConfig(config: TelemetryConfig) {
        val migrated = TelemetryConfig.migrateLegacy(config)
        configRepository.setJson(JsonStoreKeys.TELEMETRY_CONFIG, json.encodeToString(TelemetryConfig.serializer(), migrated))
        updateMvpProtocolCache(migrated.useMvpProtocol)
    }

    suspend fun loadMachineRegistration(): MachineRegistration {
        val raw = configRepository.getJson(JsonStoreKeys.MACHINE_REGISTRATION) ?: return MachineRegistration.migrateLegacy(MachineRegistration())
        return runCatching { json.decodeFromString<MachineRegistration>(raw) }.getOrDefault(MachineRegistration()).let {
            MachineRegistration.migrateLegacy(it)
        }
    }

    suspend fun saveMachineRegistration(reg: MachineRegistration) {
        configRepository.setJson(JsonStoreKeys.MACHINE_REGISTRATION, json.encodeToString(MachineRegistration.serializer(), reg))
    }

 /**
 * Регистрация машины: MVP enroll или legacy regKey.
 */
    suspend fun registerMachine(
        regKey: String,
        serialNumber: String,
        rebind: Boolean = false,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (loadTelemetryConfig().useMvpProtocol) {
                return@withContext mvpCoordinator.enrollMachine(serialNumber, rebind)
            }
            runCatching {
                val t = loadTelemetryConfig()
                val url = "${t.apiUrl.trimEnd('/')}/api/telemetry-machine-control/machine/registration/${regKey.trim()}"
                Timber.d("WivaTelemetry: POST registration url=$url serial=$serialNumber")
                val response =
                    telemetryApi.registerMachine(
                        url = url,
                        body =
                            MachineRegistrationRequestDto(
                                modelName = "WIVA",
                                machineName = "WIVA",
                                serialNumber = serialNumber.trim(),
                            ),
                    )
                if (!response.isSuccessful) {
                    error("Registration failed: ${response.code()} ${response.errorBody()?.string()}")
                }
                val body = response.body() ?: error("Empty body")
                val updated =
                    loadMachineRegistration().copy(
                        regKey = regKey.trim(),
                        serialNumber = serialNumber.trim(),
                        machineKey = body.secretKey,
                        isRegistered = true,
                    )
                saveMachineRegistration(updated)
                Timber.i("WivaTelemetry: secretKey сохранён, type=${body.type}")
            }
        }

    suspend fun reserveFreeSerial(): Result<String> = mvpCoordinator.reserveFreeSerial()

    fun connect() {
        telemetryPausedByUser = false
        scheduledAutoConnect?.cancel()
        scheduledAutoConnect = null
        scope.launch {
            if (cachedUseMvpProtocol) {
                mvpCoordinator.connect()
            } else {
                wsManager.disconnect()
                connectWithLoadedCredentials("явное подключение")
            }
        }
    }

    fun disconnect() {
        telemetryPausedByUser = true
        scheduledAutoConnect?.cancel()
        scheduledAutoConnect = null
        scope.launch {
            if (cachedUseMvpProtocol) {
                mvpCoordinator.disconnect()
            } else {
                wsManager.disconnect()
            }
        }
    }

    fun reconnect() {
        telemetryPausedByUser = false
        scheduledAutoConnect?.cancel()
        scheduledAutoConnect = null
        scope.launch {
            if (cachedUseMvpProtocol) {
                mvpCoordinator.reconnect()
            } else {
                wsManager.disconnect()
                connectWithLoadedCredentials("reconnect")
            }
        }
    }

 /**
 * Автоподключение при старте приложения и общая ветка для [connect]/[reconnect].
 * Не вызывается, если пользователь нажал «Отключить» ([telemetryPausedByUser]).
 */
    private suspend fun startTelemetryIfRegistered(reason: String) {
        if (telemetryPausedByUser) {
            Timber.d("WivaTelemetry: автоподключение пропущено ($reason) — пауза по запросу пользователя")
            return
        }
        if (cachedUseMvpProtocol) {
            mvpCoordinator.connect()
            return
        }
        connectWithLoadedCredentials(reason)
    }

    private fun isMvpProtocolActive(): Boolean = cachedUseMvpProtocol

    private suspend fun skipLegacyTopic(operation: String): Boolean {
        if (isMvpProtocolActive()) {
            Timber.i("WivaTelemetry: $operation пропущен — активен MVP-протокол")
            return true
        }
        return false
    }

    private suspend fun connectWithLoadedCredentials(reason: String) {
        if (telemetryPausedByUser) return
        val t = loadTelemetryConfig()
        var reg = loadMachineRegistration()

 // Авто-регистрация: есть regKey/serial, но ещё нет machineKey.
        if (reg.machineKey.isBlank() && reg.regKey.isNotBlank() && reg.serialNumber.isNotBlank()) {
            Timber.i("WivaTelemetry: machineKey отсутствует — авто-регистрация ($reason)")
            registerMachine(reg.regKey, reg.serialNumber)
                .onSuccess { reg = loadMachineRegistration() }
                .onFailure { Timber.e(it, "WivaTelemetry: авто-регистрация не удалась") }
        }

        if (reg.machineKey.isBlank() || reg.serialNumber.isBlank()) {
            Timber.w("WivaTelemetry: нет machineKey или serialNumber ($reason) — регистрация в сервисном меню")
            return
        }
        Timber.i("WivaTelemetry: запуск WS ($reason), url=${t.wsUrl.ifBlank { TelemetryConfig.DEFAULT_WS_URL }}")
        wsManager.connect(t.wsUrl.ifBlank { TelemetryConfig.DEFAULT_WS_URL }) {
            WivaTelemetryAuth.fetchAccessToken(
                okHttpClient,
                t.keycloakUrl.trimEnd('/'),
                t.keycloakRealm,
                clientSecret = reg.machineKey,
                clientId = reg.serialNumber,
            ).getOrNull()
        }
    }

    private suspend fun isPingPongEnabled(): Boolean =
        configRepository.get(JsonStoreKeys.TELEMETRY_PING_PONG_ENABLED) == "true"

    private suspend fun sendInitialExchange() {
        val serial = loadMachineRegistration().serialNumber.ifBlank { return }
        if (isPingPongEnabled()) {
            sendCapabilities(serial).onFailure { Timber.w(it, "capabilities (initial)") }
        } else {
            Timber.i("WivaTelemetry: ping/pong отключён — capabilities не отправляем")
        }
        requestCellStoreMatrix().onFailure { Timber.w(it, "cellStoreRequestExport (initial)") }
        requestMachineInfo().onFailure { Timber.w(it, "machineInfo (initial)") }
        requestBaseIngredients().onFailure { Timber.w(it, "baseIngredientRequestExportTopic (initial)") }
        reportKioskDeviceLocationIfPossible(serial)
    }

 /** Объявляет серверу поддерживаемые возможности (ping/pong). */
    private suspend fun sendCapabilities(serial: String): Result<Unit> {
        val payload = buildJsonObject {
            put("type", JsonPrimitive("capabilities"))
            put("clientId", JsonPrimitive(serial))
            put("body", buildJsonObject {
                put("pingPong", JsonPrimitive(true))
            })
        }
        return wsManager.sendRawJson(payload.toString())
    }

 /** Запрос матрицы наполнения. */
    suspend fun requestCellStoreMatrix(): Result<Unit> {
        if (skipLegacyTopic("cellStoreRequestExport")) return Result.success(Unit)
        val serial = loadMachineRegistration().serialNumber.ifBlank {
            return Result.failure(IllegalStateException("Нет serialNumber"))
        }
        return wsManager.sendRawJson(jsonWs.encodeToString(CellStoreRequestExportOut(clientId = serial)))
    }

 /** Запрос базы ингредиентов. */
    suspend fun requestBaseIngredients(): Result<Unit> {
        if (skipLegacyTopic("baseIngredientRequestExportTopic")) return Result.success(Unit)
        val serial = loadMachineRegistration().serialNumber.ifBlank {
            return Result.failure(IllegalStateException("Нет serialNumber"))
        }
        return wsManager.sendRawJson(jsonWs.encodeToString(BaseIngredientRequestOut(clientId = serial)))
    }

 /** Повторный запрос machineInfo. */
    suspend fun requestMachineInfo(): Result<Unit> {
        if (skipLegacyTopic("machineInfo")) return Result.success(Unit)
        val serial = loadMachineRegistration().serialNumber.ifBlank {
            return Result.failure(IllegalStateException("Нет serialNumber"))
        }
        return wsManager.sendRawJson(jsonWs.encodeToString(MachineInfoRequestOut(clientId = serial)))
    }

 /** WS `kioskDeviceLocationReport` → telemetry-machine-ws → machine-control (. */
    private suspend fun reportKioskDeviceLocationIfPossible(serial: String) {
        val loc = deviceLocationReader.readLastKnownOrNull()
        if (loc == null) {
            Timber.d("WivaTelemetry: kioskDeviceLocationReport пропущен (нет точки или разрешений)")
            return
        }
 // body — JSON-объект, не строка: telemetry-machine-ws getMessageFromByte делает json.Marshal(body);
 // для строки Marshal кодирует повторно и ломает Kafka/HTTP downstream.
        val bodyJson =
            buildJsonObject {
                put("latitude", JsonPrimitive(loc.latitude))
                put("longitude", JsonPrimitive(loc.longitude))
                loc.accuracyMeters?.let { put("accuracyMeters", JsonPrimitive(it)) }
                loc.capturedAtEpochMillis?.let { put("capturedAtEpochMillis", JsonPrimitive(it)) }
            }
        val payload =
            buildJsonObject {
                put("type", JsonPrimitive("kioskDeviceLocationReport"))
                put("clientId", JsonPrimitive(serial))
                put("body", bodyJson)
            }
        wsManager
            .sendRawJson(payload.toString())
            .fold(
                onSuccess = { Timber.i("WivaTelemetry: kioskDeviceLocationReport отправлен") },
                onFailure = { Timber.w(it, "kioskDeviceLocationReport") },
            )
    }

 /**
 * Отправляет данные о состоянии автомата (температура, версия) по топику `setMachineInfo`.
 * Вызывается только при изменении значений температуры.
 */
    suspend fun sendSetMachineInfo(temperature0: Int, temperature1: Int): Result<Unit> {
        if (skipLegacyTopic("setMachineInfo")) return Result.success(Unit)
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            return Result.success(Unit)
        }
        val serial = loadMachineRegistration().serialNumber.trim().ifBlank {
            Timber.w("WivaTelemetry: нет serialNumber для setMachineInfo")
            return Result.success(Unit)
        }
        val iso = TelemetryIsoTimestamps.nowUtc()
 //. Иначе WS-сервер
 // (getMessageFromByte + json.Marshal(interface{})) дважды кодирует строковый body.
        val payload =
            buildJsonObject {
                put("type", JsonPrimitive("setMachineInfo"))
                put("clientId", JsonPrimitive(serial))
                put(
                    "body",
                    buildJsonObject {
                        put("timestamp", JsonPrimitive(iso))
                        put("temperature", JsonPrimitive(temperature0))
                        put("temperature1", JsonPrimitive(temperature1))
                        put("versionName", JsonPrimitive(BuildConfig.VERSION_NAME))
                        put("versionCode", JsonPrimitive(BuildConfig.VERSION_CODE))
                    },
                )
            }
        return wsManager.sendRawJson(payload.toString())
    }

    suspend fun sendSaleImportTopic(message: SaleImportOutboundMessage): Result<Unit> {
        if (skipLegacyTopic("saleImportTopic")) return Result.success(Unit)
        val payload = jsonWs.encodeToString(SaleImportOutboundMessage.serializer(), message)
        return wsManager.sendRawJson(payload)
    }

 /**
 * Продажа(и) в телеметрию — тело.ts] `sendSaleImportTopic`.
 * При offline — предупреждение в лог, без исключения.
 */
    suspend fun sendSaleImportTopic(items: List<SaleImportItem>): Result<Unit> {
        if (skipLegacyTopic("saleImportTopic")) return Result.success(Unit)
        if (items.isEmpty()) return Result.success(Unit)
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен, отправка saleImportTopic невозможна")
            return Result.success(Unit)
        }
        val serial =
            loadMachineRegistration().serialNumber.trim().ifBlank {
                Timber.w("WivaTelemetry: нет serialNumber для saleImportTopic")
                return Result.success(Unit)
            }
        val reg = loadMachineRegistration()
        val orgId = reg.organizationId.toIntOrNull() ?: 0
        val machineId = reg.machineId.toIntOrNull() ?: 0
        val iso = TelemetryIsoTimestamps.nowUtc()
        val jsonItems = items.mapNotNull { line -> mapSaleImportItem(line, orgId, machineId, iso) }
        if (jsonItems.isEmpty()) return Result.success(Unit)
        return sendSaleImportTopic(SaleImportOutboundMessage(clientId = serial, body = jsonItems))
    }

    private suspend fun mapSaleImportItem(
        line: SaleImportItem,
        orgId: Int,
        machineId: Int,
        dateSale: String,
    ): SaleImportItemJson? {
        val container =
            machineInventoryRepository.findDrinkContainerByProductId(line.drinkId) ?: run {
                Timber.w("saleImportTopic: контейнер не найден drinkId=${line.drinkId}")
                return null
            }
        val dosage = container.product.dosage
        if (dosage.drinkVolume <= 0) return null
        val ratio = line.volume.toDouble() / dosage.drinkVolume
        val writeOffVol = ceil(dosage.product * ratio).toInt()
        val name = "${container.product.name} ${container.product.taste.name}"
        val isFree = line.payMethod == null
        val charged = line.totalChargedRub ?: line.price
        val totalPriceJson = if (isFree) 0.0 else charged
        val payments =
            if (isFree) {
                emptyList()
            } else {
                listOf(SaleImportPaymentJson(price = charged, method = line.payMethod!!))
            }
        return SaleImportItemJson(
            dateSale = dateSale,
            orgId = orgId,
            machineId = machineId,
            promocodeId = null,
            name = name,
            volume = line.volume,
            discountId = null,
            price = line.price,
            totalPrice = totalPriceJson,
            unit = "ML",
            writeOffs =
                listOf(
                    SaleImportWriteOffJson(
                        cellNumber = container.containerNumber,
                        ingredientId = container.product.id,
                        volume = writeOffVol,
                    ),
                ),
            payments = payments,
        )
    }

 /**
 * Остатки по ячейкам из merge-конфига.
 * @return true, если WS подключён и кадр ушёл.
 */
    suspend fun sendCellVolumeImportFromConfig(): Boolean {
        if (skipLegacyTopic("cellVolumeImportTopic")) return false
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен, отправка cellVolumeImportTopic невозможна")
            return false
        }
        val serial =
            loadMachineRegistration().serialNumber.trim().ifBlank {
                Timber.w("WivaTelemetry: нет serialNumber для cellVolumeImportTopic")
                return false
            }
        val raw =
            configRepository.getJson(JsonStoreKeys.TELEMETRY_MERGED_INVENTORY) ?: run {
                Timber.w("WivaTelemetry: нет merge-конфига для cellVolumeImportTopic")
                return false
            }
        val config =
            runCatching { json.decodeFromString(StoredMachineConfigWire.serializer(), raw) }.getOrElse {
                Timber.e(it, "WivaTelemetry: parse merge для cellVolumeImportTopic")
                return false
            }
        val volumeByNumber = config.containers.associate { it.containerNumber to (it.volume ?: 0) }
        val bodyJson =
            buildJsonObject {
                putJsonArray("cells") {
                    for (i in 1..6) {
                        addJsonObject {
                            put("number", JsonPrimitive(i))
                            put("volume", JsonPrimitive(volumeByNumber[i] ?: 0))
                        }
                    }
                }
                putJsonArray("cellCups") {}
                putJsonArray("cellWaters") {}
                putJsonArray("cellDisposables") {}
            }
        val envelope =
            buildJsonObject {
                put("clientId", JsonPrimitive(serial))
                put("type", JsonPrimitive("cellVolumeImportTopic"))
                put("body", bodyJson)
            }
        val payload = jsonWs.encodeToString(JsonObject.serializer(), envelope)
        return wsManager.sendRawJson(payload).isSuccess
    }

 /**
 * Матрица наполнения из merge + сохранённая матрица.
 */
    suspend fun sendCellStoreImportFromConfig(): Boolean {
        if (skipLegacyTopic("cellStoreImportTopic")) return false
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен, отправка cellStoreImportTopic невозможна")
            return false
        }
        val serial =
            loadMachineRegistration().serialNumber.trim().ifBlank {
                Timber.w("WivaTelemetry: нет serialNumber для cellStoreImportTopic")
                return false
            }
        val mergedRaw =
            configRepository.getJson(JsonStoreKeys.TELEMETRY_MERGED_INVENTORY) ?: run {
                Timber.w("WivaTelemetry: нет merge для cellStoreImportTopic")
                return false
            }
        val merged =
            runCatching { json.decodeFromString(StoredMachineConfigWire.serializer(), mergedRaw) }.getOrElse {
                Timber.e(it, "WivaTelemetry: parse merge для cellStoreImportTopic")
                return false
            }
        val matrixRaw = configRepository.getJson(JsonStoreKeys.TELEMETRY_CELL_STORE_MATRIX)
        val matrix =
            matrixRaw?.let { r ->
                runCatching { json.decodeFromString(CellStoreMatrixBodyWire.serializer(), r) }.getOrNull()
            } ?: CellStoreMatrixBodyWire()
        val body = CellStoreImportBodyBuilder.buildFromMerged(merged, matrix)
        val msg = CellStoreImportTopicEnvelope(clientId = serial, body = body)
        val payload = jsonWs.encodeToString(CellStoreImportTopicEnvelope.serializer(), msg)
        return wsManager.sendRawJson(payload).isSuccess
    }

 /**
 * D4 / E2E: один напиток, структура полей.
 */
    suspend fun sendDemoSaleImportForE2e(): Result<Unit> {
        val reg = loadMachineRegistration()
        val serial = reg.serialNumber.ifBlank { return Result.failure(IllegalStateException("Нет serialNumber")) }
        val orgId = reg.organizationId.toIntOrNull() ?: 0
        val machineId = reg.machineId.toIntOrNull() ?: 0
        val item =
            SaleImportItemJson(
                dateSale = "2026-04-01T12:00:00.000Z",
                orgId = orgId,
                machineId = machineId,
                promocodeId = null,
                name = "Demo drink",
                volume = 200,
                discountId = null,
                price = 1.0,
                totalPrice = 1.0,
                unit = "ML",
                writeOffs =
                    listOf(
                        SaleImportWriteOffJson(
                            cellNumber = 1,
                            ingredientId = 1,
                            volume = 200,
                        ),
                    ),
                payments =
                    listOf(
                        SaleImportPaymentJson(price = 1.0, method = "CARD"),
                    ),
            )
        return sendSaleImportTopic(SaleImportOutboundMessage(clientId = serial, body = listOf(item)))
    }

    suspend fun sendAuthCodeRequest(code: String): AuthCodeResult {
        if (skipLegacyTopic("authCodeRequestExport")) return AuthCodeResult(false, "Legacy topic отключён (MVP)")
        val reg = loadMachineRegistration()
        val serial = reg.serialNumber.ifBlank { return AuthCodeResult(false, "Нет serialNumber") }
        if (pendingAuth != null) return AuthCodeResult(false, "Запрос авторизации уже выполняется")
        val deferred = CompletableDeferred<AuthCodeResult>()
        pendingAuth = deferred
        val iso = TelemetryIsoTimestamps.nowUtc()
        val body =
            jsonWs.encodeToString(
                AuthCodeRequestOut(
                    clientId = serial,
                    body = AuthCodeBodyOut(code = code, date = iso),
                ),
            )
        wsManager.sendRawJson(body).onFailure {
            pendingAuth = null
            return AuthCodeResult(false, it.message ?: "send failed")
        }
        val result =
            withTimeoutOrNull(30_000) {
                deferred.await()
            }
        if (result == null) {
            pendingAuth = null
            return AuthCodeResult(false, "Таймаут ответа authCodeRequestExport")
        }
        return result
    }

 /** Запрос статуса подписки по UUID клиента (карте). */
    suspend fun sendStatusSubscribeTopic(userUuid: String): Result<Unit> {
        if (skipLegacyTopic("statusSubscribeTopic")) return Result.success(Unit)
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен — statusSubscribeTopic не отправлен (userUuid=%s)", userUuid)
            return Result.success(Unit)
        }
        val serial = loadMachineRegistration().serialNumber.trim().ifBlank {
            Timber.w("WivaTelemetry: нет serialNumber для statusSubscribeTopic")
            return Result.success(Unit)
        }
        val body = userUuid.trim()
        if (body.isBlank()) return Result.failure(IllegalArgumentException("Пустой userUuid"))
        val payload =
            jsonWs.encodeToString(
                StatusSubscribeTopicRequest(
                    clientId = serial,
                    body = body,
                ),
            )
        return wsManager.sendRawJson(payload)
    }

 /** Запрос тарифов подписки (orgId из machineInfo). */
    suspend fun sendSubscriptionLevelRequest(): Result<Unit> {
        if (skipLegacyTopic("subscriptionLevelTopic")) return Result.success(Unit)
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен — subscriptionLevelTopic не отправлен (тарифы не придут)")
            return Result.success(Unit)
        }
        val reg = loadMachineRegistration()
        val serial = reg.serialNumber.trim().ifBlank {
            Timber.w("WivaTelemetry: нет serialNumber для subscriptionLevelTopic")
            return Result.success(Unit)
        }
        val orgId = reg.organizationId.toIntOrNull() ?: 0
        val payload =
            jsonWs.encodeToString(
                SubscriptionLevelTopicRequest(
                    clientId = serial,
                    body = orgId,
                ),
            )
        return wsManager.sendRawJson(payload)
    }

 /**
 * Скан карты лояльности `CLIENT_<uuid>`: параллельно по смыслу с 
 * `checkSubscriptionStatus` + `requestSubscriptionLevels` (оба WS-сообщения подряд).
 */
    fun onLoyaltyCardScanned(clientUuid: String) {
        val id = clientUuid.trim()
        if (id.isEmpty()) return
        scope.launch {
 // Сразу сбрасываем прошлый ответ subscriptionLevelTopic — UI показывает загрузку до нового списка.
            _subscriptionLevels.value = null
            _loyaltyCardClientScans.emit(id)
            sendStatusSubscribeTopic(id)
            sendSubscriptionLevelRequest()
        }
    }

 /**
 * Сброс карточки подписки в UI (как `setSubscribeInfo(null)`.
 * Очищает replay последнего скана, чтобы после «Выход» новый подписчик не подхватывал старый UUID.
 */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearSubscribeUiState() {
        _subscribeInfo.value = null
        _subscriptionLevels.value = null
        _loyaltyCardClientScans.resetReplayCache()
    }

 /** Отправка покупки/отмены подписки (saleSubscribeTopic). */
    suspend fun sendSaleSubscribeTopic(body: SaleSubscribeTopicBody): Result<Unit> {
        if (skipLegacyTopic("saleSubscribeTopic")) return Result.success(Unit)
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен, отправка saleSubscribeTopic невозможна")
            return Result.success(Unit)
        }
        val serial = loadMachineRegistration().serialNumber.trim().ifBlank {
            Timber.w("WivaTelemetry: нет serialNumber для saleSubscribeTopic")
            return Result.success(Unit)
        }
        val payload =
            jsonWs.encodeToString(
                SaleSubscribeTopicRequest(
                    clientId = serial,
                    body = body,
                ),
            )
        return wsManager.sendRawJson(payload)
    }

 /** Таймер ожидания ответа на SALE; при таймауте отправляем CANCEL. */
    fun startSubscriptionSaleTimer(
        requestUuid: String,
        machineClientId: String,
        userUuid: String,
        machineId: Int,
    ) {
        clearSubscriptionSaleTimer(requestUuid)
        val job =
            scope.launch {
                delay(SUBSCRIPTION_SALE_TIMEOUT_MS)
                subscriptionSaleTimers.remove(requestUuid)
                sendSaleSubscribeTopic(
                    SaleSubscribeTopicBody(
                        machineClientId = machineClientId,
                        userUuid = userUuid,
                        machineId = machineId,
                        requestUuid = requestUuid,
                        operationType = SaleSubscribeOperationType.CANCEL,
                    ),
                )
                Timber.i("saleSubscribeTopic: timeout, sent CANCEL requestUuid=$requestUuid")
            }
        subscriptionSaleTimers[requestUuid] = job
    }

    fun clearSubscriptionSaleTimer(requestUuid: String) {
        subscriptionSaleTimers.remove(requestUuid)?.cancel()
    }

 /** useSubscriptionSaleTopic: учёт налива по карте подписки. */
    suspend fun sendUseSubscriptionSaleTopic(body: UseSubscriptionSaleBody): Result<Unit> {
        if (skipLegacyTopic("useSubscriptionSaleTopic")) return Result.success(Unit)
        if (wsManager.connectionState.value !is ConnectionState.Connected) {
            Timber.w("WivaTelemetry: WebSocket не подключен, отправка useSubscriptionSaleTopic невозможна")
            return Result.success(Unit)
        }
        val serial = loadMachineRegistration().serialNumber.trim().ifBlank {
            Timber.w("WivaTelemetry: нет serialNumber для useSubscriptionSaleTopic")
            return Result.success(Unit)
        }
        val payload =
            jsonWs.encodeToString(
                UseSubscriptionSaleTopicRequest(
                    clientId = serial,
                    body = body,
                ),
            )
        return wsManager.sendRawJson(payload)
    }

    private suspend fun handleIncoming(frame: WivaWsIncomingFrame) {
        when (frame.type) {
            "machineInfo" -> handleMachineInfo(frame.rawJson)
            "authCodeRequestExport" -> handleAuthCodeExport(frame.rawJson)
            "cellStoreExport" -> {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        machineInventoryRepository.ingestCellStoreMessage(frame.rawJson)
                    }
                    Timber.d("WivaTelemetry IN: cellStoreExport → merge наполнения")
 // На первом подключении сервер возвращает только ACK на baseIngredientRequestExportTopic.
 // После прихода матрицы (cellStoreExport) проверяем и при необходимости повторно запрашиваем базу.
                    if (!machineInventoryRepository.isBaseIngredientLoaded()) {
                        Timber.w("WivaTelemetry: база ингредиентов не загружена, повторный запрос")
                        requestBaseIngredients().onFailure {
                            Timber.w(it, "WivaTelemetry: retry baseIngredientRequestExportTopic")
                        }
                    }
                }
            }
            "cellStoreRequestExport" -> {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        machineInventoryRepository.ingestCellStoreMessage(frame.rawJson)
                    }
                    Timber.d("WivaTelemetry IN: cellStoreRequestExport → merge наполнения")
                }
            }
            "baseIngredientRequestExportTopic" -> {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        machineInventoryRepository.ingestBaseIngredientExport(frame.rawJson)
                    }
                    Timber.d("WivaTelemetry IN: baseIngredient → merge с матрицей")
                }
            }
            "cellVolumeExport" -> {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        machineInventoryRepository.ingestCellVolumeExport(frame.rawJson)
                    }
                    Timber.d("WivaTelemetry IN: cellVolumeExport → остатки по ячейкам")
                }
            }
            "subscribeInformationTopic" -> handleSubscribeInformation(frame.rawJson)
            "subscriptionLevelTopic" -> handleSubscriptionLevel(frame.rawJson)
            "machineVersionChangeExport",
            -> {
                Timber.d("WivaTelemetry IN: type=${frame.type} (без локального разбора)")
            }
            else ->
                if (frame.type != null) {
                    Timber.d("WivaTelemetry IN: неизвестный type=${frame.type}")
                }
        }
    }

    private suspend fun handleMachineInfo(raw: String) {
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val body = root["body"]?.jsonObject ?: return@runCatching
            val id = body["id"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val organizationId = body["organizationId"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val modelId = body["modelId"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val serialNumber = body["serialNumber"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val current = loadMachineRegistration()
            saveMachineRegistration(
                current.copy(
                    machineId = id,
                    organizationId = organizationId,
                    modelId = modelId,
                    serialNumber = serialNumber,
                ),
            )
            Timber.i("WivaTelemetry: machineInfo сохранён id=$id org=$organizationId")
        }.onFailure { Timber.e(it, "machineInfo parse") }
    }

    private fun handleAuthCodeExport(raw: String) {
        val pending = pendingAuth ?: return
        pendingAuth = null
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val success = root["success"]?.jsonPrimitive?.booleanOrNull ?: false
            val message = root["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
            pending.complete(AuthCodeResult(success, message))
        }.onFailure {
            pending.complete(AuthCodeResult(false, it.message ?: "parse error"))
        }
    }

    private fun handleSubscribeInformation(raw: String) {
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val body = root["body"]?.jsonObject ?: return@runCatching
            val clientId = body["clientId"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (clientId.isBlank()) {
                clearSubscribeUiState()
                scope.launch { _invalidLoyaltyCardScans.emit(Unit) }
                return@runCatching
            }
            val requestUuid = body["requestUuid"]?.jsonPrimitive?.contentOrNull
            val operationType = body["operationType"]?.jsonPrimitive?.contentOrNull
            if (!requestUuid.isNullOrBlank() && !operationType.isNullOrBlank()) {
                clearSubscriptionSaleTimer(requestUuid)
            }

            fun parseLitersAsMl(key: String): Int {
                val rawNum = body[key]?.jsonPrimitive?.contentOrNull ?: return 0
                val liters = rawNum.toDoubleOrNull() ?: return 0
                return (liters * 1000.0).toInt()
            }

            _subscribeInfo.value =
                SubscribeInformationState(
                    isStatusRequest = body["isStatusRequest"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isActiveSubscribe = body["isActiveSubscribe"]?.jsonPrimitive?.booleanOrNull ?: false,
                    clientId = clientId,
                    subscribeDateEnd = body["subscribeDateEnd"]?.jsonPrimitive?.contentOrNull,
                    volumeMl = parseLitersAsMl("volume"),
                    maxVolumeMl = parseLitersAsMl("maxVolume"),
                    requestUuid = requestUuid,
                    operationType = operationType,
                )
        }.onFailure {
            Timber.w(it, "subscribeInformationTopic parse")
        }
    }

 /**
 * Лимит в литрах из subscriptionLevelTopic: JSON number / double / строка; ключи volume, maxVolume, limit.
 * Раньше только string→int — пропадали числовые и дробные значения (например Premium).
 */
    private fun parseSubscriptionVolumeLiters(o: JsonObject): Int? {
        for (key in listOf("volume", "maxVolume", "limit")) {
            val prim = o[key] as? JsonPrimitive ?: continue
            prim.intOrNull?.takeIf { it > 0 }?.let { return it }
            prim.doubleOrNull?.takeIf { it > 0 }?.let { return it.roundToInt().coerceAtLeast(1) }
            prim.contentOrNull
                ?.toDoubleOrNull()
                ?.takeIf { it > 0 }
                ?.let { return it.roundToInt().coerceAtLeast(1) }
        }
        return null
    }

    private fun handleSubscriptionLevel(raw: String) {
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val bodyArray = root["body"]?.jsonArray ?: return@runCatching
            val items =
                bodyArray.mapNotNull { el ->
                    val o = el.jsonObject
                    val uuid = o["uuid"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val price = o["price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
                    SubscriptionLevelItem(
                        uuid = uuid,
                        price = price,
                        name = o["name"]?.jsonPrimitive?.contentOrNull,
                        volume = parseSubscriptionVolumeLiters(o),
                        orgId = o["orgId"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                    )
                }
 // Как subscriptionLevelsStore.
            _subscriptionLevels.value =
                items.sortedWith(compareBy({ it.price }, { it.uuid }))
        }.onFailure {
            Timber.w(it, "subscriptionLevelTopic parse")
        }
    }

    @Serializable
    private data class CellStoreRequestExportOut(
        val type: String = "cellStoreRequestExport",
        val clientId: String,
    )

    @Serializable
    private data class MachineInfoRequestOut(
        val clientId: String,
        val type: String = "machineInfo",
        val body: JsonObject = buildJsonObject { },
    )

    @Serializable
    private data class BaseIngredientRequestOut(
        val clientId: String,
        val type: String = "baseIngredientRequestExportTopic",
    )

    @Serializable
    private data class AuthCodeRequestOut(
        val clientId: String,
        val type: String = "authCodeRequestExport",
        val body: AuthCodeBodyOut,
    )

    @Serializable
    private data class AuthCodeBodyOut(
        val code: String,
        val date: String,
    )

    @Serializable
    private data class CellStoreImportTopicEnvelope(
        val type: String = "cellStoreImportTopic",
        val clientId: String,
        val body: CellStoreMatrixBodyWire,
    )

}
