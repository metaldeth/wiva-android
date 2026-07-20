package com.viwa.android.ui.screens.service.tabs

import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViwaCardPaymentMethodViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo(
        var selected: CardPaymentMethod = CardPaymentMethod.Aqsi,
    ) : CardPaymentMethodRepository {
        override suspend fun getSelected(): CardPaymentMethod = selected

        override suspend fun setSelected(method: CardPaymentMethod) {
            selected = method
        }
    }

    private class FakeMockRepo(
        var mode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    ) : CardPaymentMockModeRepository {
        override suspend fun getMode(): CardPaymentMockMode = mode

        override suspend fun setMode(mode: CardPaymentMockMode) {
            this.mode = mode
        }

        override suspend fun getOutcome() = com.viwa.android.domain.model.CardPaymentMockOutcome.Approved

        override suspend fun setOutcome(outcome: com.viwa.android.domain.model.CardPaymentMockOutcome) = Unit
    }

    @Test
    fun refresh_loadsAqsiByDefault() {
        val fake = FakeRepo(CardPaymentMethod.Aqsi)
        val vm = ViwaCardPaymentMethodViewModel(fake, FakeMockRepo(CardPaymentMockMode.Aqsi))
        assertEquals(CardPaymentMethod.Aqsi, vm.uiState.value.selected)
        assertEquals(CardPaymentMockMode.Aqsi, vm.uiState.value.mockMode)
        assertEquals(false, vm.uiState.value.isBusy)
    }
}
