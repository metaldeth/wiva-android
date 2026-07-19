package com.viwa.android.ui.screens.service.tabs

import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
        var selected: CardPaymentMethod = CardPaymentMethod.Pax,
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

        override suspend fun getOutcome(): CardPaymentMockOutcome = CardPaymentMockOutcome.Approved

        override suspend fun setOutcome(outcome: CardPaymentMockOutcome) = Unit
    }

    @Test
    fun initialLoad_whenRepositoryAQSI_uiReflectsAqsi() =
        runBlocking {
            val fake = FakeRepo(CardPaymentMethod.Aqsi)
            val vm = ViwaCardPaymentMethodViewModel(fake, FakeMockRepo(CardPaymentMockMode.Aqsi))
            assertEquals(CardPaymentMethod.Aqsi, vm.uiState.value.selected)
            assertEquals(CardPaymentMockMode.Aqsi, vm.uiState.value.mockMode)
        }

    @Test
    fun selectPax_callsSetSelectedPax() =
        runBlocking {
            val fake = FakeRepo(CardPaymentMethod.Aqsi)
            val vm = ViwaCardPaymentMethodViewModel(fake, FakeMockRepo())
            vm.selectPax()
            assertEquals(CardPaymentMethod.Pax, fake.selected)
            assertEquals(CardPaymentMethod.Pax, vm.uiState.value.selected)
        }

    @Test
    fun selectAqsi_callsSetSelectedAqsi() =
        runBlocking {
            val fake = FakeRepo(CardPaymentMethod.Pax)
            val vm = ViwaCardPaymentMethodViewModel(fake, FakeMockRepo())
            vm.selectAqsi()
            assertEquals(CardPaymentMethod.Aqsi, fake.selected)
            assertEquals(CardPaymentMethod.Aqsi, vm.uiState.value.selected)
        }
}
