package com.wiva.android.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel
@Inject
constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {
    val isDark: StateFlow<Boolean> = themeRepository.isDark
    val customerPrimaryLightArgb: StateFlow<Int> = themeRepository.customerPrimaryLightArgb
    val customerPrimaryDarkArgb: StateFlow<Int> = themeRepository.customerPrimaryDarkArgb

    init {
        viewModelScope.launch { themeRepository.init() }
    }
}
