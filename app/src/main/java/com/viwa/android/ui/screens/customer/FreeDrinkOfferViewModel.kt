package com.viwa.android.ui.screens.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viwa.android.services.telemetry.ViwaTelemetryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Базовый URL для QR регистрации подписки. */
private const val DEFAULT_SUBSCRIPTION_QR_BASE_URL = "http://dev.ishaker.ru:3005/main"

@HiltViewModel
class FreeDrinkOfferViewModel
    @Inject
    constructor(
        private val telemetryService: ViwaTelemetryService,
    ) : ViewModel() {
        private val _qrUrl = MutableStateFlow<String?>(null)
        val qrUrl: StateFlow<String?> = _qrUrl.asStateFlow()

        init {
            viewModelScope.launch {
                val reg = telemetryService.loadMachineRegistration()
                val orgId = reg.organizationId.toIntOrNull()
                val machineId = reg.machineId.toIntOrNull()
                if (orgId != null && machineId != null && orgId > 0 && machineId > 0) {
                    _qrUrl.value = "$DEFAULT_SUBSCRIPTION_QR_BASE_URL/$orgId/5/$machineId"
                }
            }
        }
    }
