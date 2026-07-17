package com.wiva.android.services.payment

import com.wiva.android.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

data class CardPaymentLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val provider: String,
    val message: String,
    val detail: String = "",
    val isError: Boolean = false,
    val lane: CardPaymentLogLane = CardPaymentLogLane.System,
)

@Singleton
class CardPaymentEventLogger
    @Inject
    constructor() {
        private val _entries = MutableStateFlow<List<CardPaymentLogEntry>>(emptyList())
        val entries: StateFlow<List<CardPaymentLogEntry>> = _entries.asStateFlow()

        fun info(
            provider: String,
            message: String,
            detail: String = "",
            lane: CardPaymentLogLane = CardPaymentLogLane.System,
        ) {
            append(provider, message, detail, isError = false, lane = lane)
        }

        fun error(
            provider: String,
            message: String,
            detail: String = "",
            lane: CardPaymentLogLane = CardPaymentLogLane.System,
        ) {
            append(provider, message, detail, isError = true, lane = lane)
        }

        fun clear() {
            _entries.value = emptyList()
        }

        private fun append(
            provider: String,
            message: String,
            detail: String,
            isError: Boolean,
            lane: CardPaymentLogLane,
        ) {
            val entry =
                CardPaymentLogEntry(
                    id = System.nanoTime(),
                    timestampMillis = System.currentTimeMillis(),
                    provider = provider,
                    message = message,
                    detail = detail.take(MAX_DETAIL_LEN),
                    isError = isError,
                    lane = lane,
                )
            _entries.update { current ->
                (listOf(entry) + current).take(MAX_ENTRIES)
            }
            if (BuildConfig.DEBUG) {
                val level = if (isError) "E" else "I"
                Timber.tag("CardPaymentLog")
                    .d("[%s] %s | %s | %s", level, provider, message, detail.take(120))
            }
        }

        private companion object {
            const val MAX_ENTRIES = 50
            const val MAX_DETAIL_LEN = 400
        }
    }
