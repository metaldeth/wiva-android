package com.viwa.android.ui.screens.service.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViwaCardPaymentMethodUiState(
    val selected: CardPaymentMethod = CardPaymentMethod.Aqsi,
    val mockMode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    val isBusy: Boolean = false,
    val banner: String? = null,
    val bannerIsError: Boolean = false,
)

@HiltViewModel
class ViwaCardPaymentMethodViewModel
    @Inject
    constructor(
        private val cardPaymentMethodRepository: CardPaymentMethodRepository,
        private val cardPaymentMockModeRepository: CardPaymentMockModeRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ViwaCardPaymentMethodUiState(isBusy = true))
        val uiState: StateFlow<ViwaCardPaymentMethodUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, banner = null) }
                runCatching {
                    cardPaymentMethodRepository.getSelected() to cardPaymentMockModeRepository.getMode()
                }.onSuccess { (method, mockMode) ->
                        _uiState.update {
                            it.copy(selected = method, mockMode = mockMode, isBusy = false, banner = null)
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = e.message ?: "Ошибка чтения метода",
                                bannerIsError = true,
                            )
                        }
                    }
            }
        }

        fun ensureAqsiSelected() {
            viewModelScope.launch {
                runCatching { cardPaymentMethodRepository.setSelected(CardPaymentMethod.Aqsi) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(selected = CardPaymentMethod.Aqsi, isBusy = false, banner = "aQsi USB — активный метод")
                        }
                    }
            }
        }
    }
