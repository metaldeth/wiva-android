package com.viwa.android.services.payment

import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.model.CardPaymentResult
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Единая точка оплаты **картой** (не СБП) с экрана заказа: напиток и подписка.
 * PAX — только делегирование в [PaymentTerminalService.sendSumToTerminal]; aQsi — [AqsiRepository.initiatePayment].
 *
 * Создаётся в [com.viwa.android.di.AqsiModule.provideCardPaymentOrchestrator] с одним [CardPaymentEventLogger] на всё приложение.
 */
class CardPaymentOrchestrator(
    private val cardPaymentMethodRepository: CardPaymentMethodRepository,
    private val paymentTerminalService: PaymentTerminalService,
    private val aqsiRepository: AqsiRepository,
    private val cardPaymentMockModeRepository: CardPaymentMockModeRepository,
    private val paymentEventLogger: CardPaymentEventLogger,
) {
    private val gate = Any()
    @Volatile private var activeBranchForCancel: CardPaymentMethod? = null

 /**
 * Параметры совпадают с [PaymentTerminalService.sendSumToTerminal] (сумма в рублях, как в приложении).
 */
    suspend fun pay(
        type: TerminalProductType,
        price: Int,
        productNumber: Int,
        sbp: Boolean,
    ): CardPaymentResult {
        val method = cardPaymentMethodRepository.getSelected()
        val mockMode = cardPaymentMockModeRepository.getMode()
        synchronized(gate) {
            activeBranchForCancel = method
        }
        Timber.tag(TAG).i(
            "pay start method=%s mock=%s type=%s price=%d product=%d sbp=%s",
            method,
            mockMode,
            type.name,
            price,
            productNumber,
            sbp,
        )
        return try {
            mockPaymentIfEnabled(method, mockMode, price)?.let { return it }
            when (method) {
                CardPaymentMethod.Pax -> payPax(type, price, productNumber, sbp)
                CardPaymentMethod.Aqsi -> payAqsi(price)
            }
        } catch (e: CancellationException) {
            Timber.tag(TAG).i("pay cancelled")
            throw e
        } finally {
            synchronized(gate) {
                if (activeBranchForCancel === method) {
                    activeBranchForCancel = null
                }
            }
            Timber.tag(TAG).i("pay finished")
        }
    }

 /**
 * Отмена активной карточной сессии: PAX — [PaymentTerminalService.cancelTransaction]; aQsi — [AqsiRepository.cancelPayment].
 * Желательно вызывать **до** [kotlinx.coroutines.Job.cancel] платёжной корутины.
 */
    suspend fun cancelActivePayment() {
        val m =
            synchronized(gate) {
                val x = activeBranchForCancel
                activeBranchForCancel = null
                x
            } ?: run {
                Timber.tag(TAG).d("cancelActivePayment: no active branch")
                return
            }
        Timber.tag(TAG).i("cancelActivePayment branch=%s", m)
        when (m) {
            CardPaymentMethod.Pax -> {
                paymentEventLogger.info(
                    "2can",
                    "Отмена активной операции",
                    lane = CardPaymentLogLane.System,
                )
                paymentTerminalService.cancelTransaction()
            }
            CardPaymentMethod.Aqsi ->
                aqsiRepository.cancelPayment().onFailure {
                    paymentEventLogger.error(
                        "Новый считыватель",
                        "Ошибка отмены",
                        it.message ?: it.javaClass.simpleName,
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    Timber.tag(TAG).w(it, "cancelActivePayment: AQSI cancel failed")
                }
        }
    }

    private suspend fun payPax(
        type: TerminalProductType,
        price: Int,
        productNumber: Int,
        sbp: Boolean,
    ): CardPaymentResult {
        return try {
            paymentTerminalService.sendSumToTerminal(type, price, productNumber, sbp)
            when (val terminalResult = paymentTerminalService.waitForPaymentResult()) {
                PaymentTerminalResult.Approved -> CardPaymentResult.Success
                PaymentTerminalResult.Declined -> CardPaymentResult.Failed("2can: оплата отклонена")
                PaymentTerminalResult.Cancelled -> CardPaymentResult.Cancelled
                PaymentTerminalResult.Timeout -> CardPaymentResult.Failed("2can: таймаут ожидания оплаты")
                is PaymentTerminalResult.Error -> CardPaymentResult.Failed(terminalResult.reason)
            }
        } catch (e: CancellationException) {
            paymentTerminalService.cancelTransaction()
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "PAX delegation failed")
            CardPaymentResult.Failed(e.message?.take(256) ?: "payment_failed")
        }
    }

    private suspend fun payAqsi(priceRub: Int): CardPaymentResult {
        val kopecks = priceRub.coerceAtLeast(0) * 100
        paymentEventLogger.info(
            "Новый считыватель",
            "Запуск оплаты",
            "$kopecks коп.",
            lane = CardPaymentLogLane.ToTerminal,
        )
        return try {
            aqsiRepository.initiatePayment(kopecks).fold(
                onSuccess = { mapAqsiToCard(it) },
                onFailure = { e ->
                    paymentEventLogger.error(
                        "Новый считыватель",
                        "Ошибка оплаты",
                        safeThrowableMessage(e),
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    Timber.tag(TAG).w(e, "AQSI initiatePayment failure")
                    CardPaymentResult.Failed(safeThrowableMessage(e))
                },
            )
        } catch (e: CancellationException) {
            runCatching { aqsiRepository.cancelPayment() }.onFailure { ex ->
                Timber.tag(TAG).w(ex, "AQSI cleanup after cancellation")
            }
            throw e
        }
    }

    private fun mapAqsiToCard(r: AqsiPaymentResult): CardPaymentResult =
        when (r) {
            AqsiPaymentResult.Approved -> {
                paymentEventLogger.info(
                    "Новый считыватель",
                    "Оплата подтверждена",
                    lane = CardPaymentLogLane.FromTerminal,
                )
                CardPaymentResult.Success
            }
            is AqsiPaymentResult.Declined -> {
                paymentEventLogger.info(
                    "Новый считыватель",
                    "Оплата отклонена",
                    r.publicCode,
                    lane = CardPaymentLogLane.FromTerminal,
                )
                CardPaymentResult.Failed(
                    if (r.publicCode.isBlank()) "declined" else "declined:${r.publicCode}",
                )
            }

            is AqsiPaymentResult.Error -> {
                paymentEventLogger.error(
                    "Новый считыватель",
                    "Ошибка оплаты",
                    r.safeMessage,
                    lane = CardPaymentLogLane.FromTerminal,
                )
                CardPaymentResult.Failed(r.safeMessage.take(256))
            }
            AqsiPaymentResult.Cancelled -> {
                paymentEventLogger.info(
                    "Новый считыватель",
                    "Оплата отменена",
                    lane = CardPaymentLogLane.FromTerminal,
                )
                CardPaymentResult.Cancelled
            }
        }

    private suspend fun mockPaymentIfEnabled(
        method: CardPaymentMethod,
        mockMode: CardPaymentMockMode,
        priceRub: Int,
    ): CardPaymentResult? {
        val matches =
            when (method) {
                CardPaymentMethod.Pax -> mockMode === CardPaymentMockMode.TwoCan
                CardPaymentMethod.Aqsi -> mockMode === CardPaymentMockMode.Aqsi
            }
        if (!matches) return null
        val outcome = cardPaymentMockModeRepository.getOutcome()
        val provider =
            when (mockMode) {
                CardPaymentMockMode.TwoCan -> "2can mock"
                CardPaymentMockMode.Aqsi -> "Новый считыватель mock"
                CardPaymentMockMode.Disabled -> return null
            }
        paymentEventLogger.info(
            provider,
            "Mock-оплата запущена",
            "$priceRub руб., сценарий ${mockOutcomeLabel(outcome)}",
            lane = CardPaymentLogLane.Mock,
        )
        delay(300)
        return when (outcome) {
            CardPaymentMockOutcome.Approved -> {
                paymentEventLogger.info(
                    provider,
                    "Mock-оплата подтверждена",
                    "Физический платёжник не используется",
                    lane = CardPaymentLogLane.Mock,
                )
                CardPaymentResult.Success
            }
            CardPaymentMockOutcome.Declined -> {
                paymentEventLogger.info(
                    provider,
                    "Mock-оплата отклонена",
                    "Тестовый отказ",
                    lane = CardPaymentLogLane.Mock,
                )
                CardPaymentResult.Failed("mock_declined")
            }
            CardPaymentMockOutcome.Cancelled -> {
                paymentEventLogger.info(
                    provider,
                    "Mock-оплата отменена",
                    "Тестовая отмена",
                    lane = CardPaymentLogLane.Mock,
                )
                CardPaymentResult.Cancelled
            }
            CardPaymentMockOutcome.Timeout -> {
                paymentEventLogger.error(
                    provider,
                    "Mock-таймаут оплаты",
                    "Тестовый таймаут",
                    lane = CardPaymentLogLane.Mock,
                )
                CardPaymentResult.Failed("mock_timeout")
            }
        }
    }

    private fun mockOutcomeLabel(outcome: CardPaymentMockOutcome): String =
        when (outcome) {
            CardPaymentMockOutcome.Approved -> "успех"
            CardPaymentMockOutcome.Declined -> "отказ"
            CardPaymentMockOutcome.Cancelled -> "отмена"
            CardPaymentMockOutcome.Timeout -> "таймаут"
        }

    private fun safeThrowableMessage(t: Throwable?): String =
        t?.message?.take(256)?.ifBlank { null } ?: t?.javaClass?.simpleName ?: "error"

    private companion object {
        const val TAG = "CardPayment"
    }
}

private object DisabledCardPaymentMockModeRepository : CardPaymentMockModeRepository {
    override suspend fun getMode(): CardPaymentMockMode = CardPaymentMockMode.Disabled

    override suspend fun setMode(mode: CardPaymentMockMode) = Unit

    override suspend fun getOutcome(): CardPaymentMockOutcome = CardPaymentMockOutcome.Approved

    override suspend fun setOutcome(outcome: CardPaymentMockOutcome) = Unit
}
