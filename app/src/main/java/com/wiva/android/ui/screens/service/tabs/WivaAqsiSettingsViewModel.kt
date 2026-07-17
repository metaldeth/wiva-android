package com.wiva.android.ui.screens.service.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiva.android.domain.model.AQSI_DEFAULT_TCP_PORT
import com.wiva.android.domain.model.AQSI_DEFAULT_TIMEOUT_MS
import com.wiva.android.domain.model.AqsiConfig
import com.wiva.android.domain.repository.AqsiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG_AQSI_SETTINGS_UI = "AqsiSettings"

data class WivaAqsiSettingsUiState(
    val host: String = "",
    val portText: String = "",
    val timeoutSecText: String = "",
    val isBusy: Boolean = false,
    val tcpTestBusy: Boolean = false,
    val banner: String? = null,
    val bannerIsError: Boolean = false,
    val tcpTestBanner: String? = null,
    val tcpTestIsError: Boolean = false,
)

@HiltViewModel
class WivaAqsiSettingsViewModel
    @Inject
    constructor(
        private val aqsiRepository: AqsiRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WivaAqsiSettingsUiState(isBusy = true))
        val uiState: StateFlow<WivaAqsiSettingsUiState> = _uiState.asStateFlow()

        init {
            reloadFromStore()
        }

        fun setHost(value: String) {
            _uiState.update { it.copy(host = value) }
        }

        fun setPortText(value: String) {
            _uiState.update { it.copy(portText = value) }
        }

        fun setTimeoutSecText(value: String) {
            _uiState.update { it.copy(timeoutSecText = value) }
        }

        fun reloadFromStore() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, banner = null) }
                runCatching { aqsiRepository.loadConfig() }
                    .onSuccess { cfg ->
                        _uiState.update {
                            it.copy(
                                host = cfg.host,
                                portText = cfg.port.toString(),
                                timeoutSecText = (cfg.timeoutMs.coerceAtLeast(1000L) / 1000L).toString(),
                                isBusy = false,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = e.message ?: "Не удалось прочитать настройки",
                                bannerIsError = true,
                            )
                        }
                    }
            }
        }

        fun save() {
            viewModelScope.launch {
                val parsed =
                    buildConfigOrReject { msg ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = msg,
                                bannerIsError = true,
                            )
                        }
                    } ?: return@launch
                _uiState.update { it.copy(isBusy = true, banner = null) }
                runCatching { aqsiRepository.saveConfig(parsed) }
                    .onSuccess {
                        Timber.tag(TAG_AQSI_SETTINGS_UI).i(
                            "save ok hostEmpty=${parsed.host.isBlank()} port=${parsed.port} timeoutMs=${parsed.timeoutMs}",
                        )
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = "Сохранено",
                                bannerIsError = false,
                            )
                        }
                    }
                    .onFailure { e ->
                        Timber.tag(TAG_AQSI_SETTINGS_UI).w(e, "save failed")
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = e.message ?: "Ошибка сохранения",
                                bannerIsError = true,
                            )
                        }
                    }
            }
        }

 /**
 * Сначала сохраняет поля из формы (репозиторий читает конфиг из хранилища), затем TCP-проверка.
 */
        fun testTcpConnection() {
            viewModelScope.launch {
                val parsed =
                    buildConfigOrReject { msg ->
                        _uiState.update {
                            it.copy(
                                tcpTestBusy = false,
                                tcpTestBanner = msg,
                                tcpTestIsError = true,
                            )
                        }
                    } ?: return@launch
                _uiState.update {
                    it.copy(tcpTestBusy = true, tcpTestBanner = null, tcpTestIsError = false)
                }
                runCatching { aqsiRepository.saveConfig(parsed) }
                    .onFailure { e ->
                        Timber.tag(TAG_AQSI_SETTINGS_UI).w(e, "save before tcp failed")
                        _uiState.update {
                            it.copy(
                                tcpTestBusy = false,
                                tcpTestBanner = e.message ?: "Не удалось сохранить перед тестом",
                                tcpTestIsError = true,
                            )
                        }
                        return@launch
                    }
                val result = runCatching { aqsiRepository.testTcpConnection() }.getOrElse {
                    Result.failure(it)
                }
                if (result.isSuccess) {
                    Timber.tag(TAG_AQSI_SETTINGS_UI).i("tcp_test ui: success")
                    _uiState.update {
                        it.copy(
                            tcpTestBusy = false,
                            tcpTestBanner = "Соединение OK",
                            tcpTestIsError = false,
                        )
                    }
                } else {
                    val msg = result.exceptionOrNull()?.message?.take(120) ?: "Ошибка TCP"
                    Timber.tag(TAG_AQSI_SETTINGS_UI).w("tcp_test ui: failure msg=%s", msg)
                    _uiState.update {
                        it.copy(
                            tcpTestBusy = false,
                            tcpTestBanner = msg,
                            tcpTestIsError = true,
                        )
                    }
                }
            }
        }

        private fun buildConfigOrReject(onInvalid: (String) -> Unit): AqsiConfig? {
            val host = _uiState.value.host.trim()
            val portText = _uiState.value.portText.trim()
            val timeoutSecText = _uiState.value.timeoutSecText.trim()

            if (host.isBlank()) {
                onInvalid("Укажите адрес хоста (FQDN или IP)")
                return null
            }

            val port =
                if (portText.isEmpty()) {
                    AQSI_DEFAULT_TCP_PORT
                } else {
                    portText.toIntOrNull()
                        ?: run {
                            onInvalid("Порт: введите число 1–65535")
                            return null
                        }
                }
            if (port !in 1..65535) {
                onInvalid("Порт вне диапазона 1–65535")
                return null
            }

            val timeoutSec: Long =
                if (timeoutSecText.isEmpty()) {
                    AQSI_DEFAULT_TIMEOUT_MS / 1000L
                } else {
                    timeoutSecText.toLongOrNull()
                        ?: run {
                            onInvalid("Таймаут (сек): введите целое число")
                            return null
                        }
                }
            if (timeoutSec !in 1..600L) {
                onInvalid("Таймаут: 1–600 с")
                return null
            }

            return AqsiConfig(
                host = host,
                port = port,
                timeoutMs = timeoutSec * 1000L,
            )
        }
    }
