package com.wiva.android.hardware.scanner

import com.wiva.android.domain.model.BarcodeEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScannerTrafficEntry(
    val id: Int,
    val timestampMs: Long,
 /** Краткая классификация для свёрнутой строки карточки. */
    val classificationLabel: String,
 /** Сырая строка со сканера (до классификации). */
    val rawLine: String,
)

@Singleton
class WivaScannerTrafficLogger
@Inject
constructor() {
    private val maxEntries = 50
    private val counter = AtomicInteger(0)

    private val _entries = MutableStateFlow<List<ScannerTrafficEntry>>(emptyList())
    val entries: StateFlow<List<ScannerTrafficEntry>> = _entries.asStateFlow()

    fun log(rawLine: String, event: BarcodeEvent) {
        val label = classificationLabel(event)
        val line =
            ScannerTrafficEntry(
                id = counter.getAndIncrement(),
                timestampMs = System.currentTimeMillis(),
                classificationLabel = label,
                rawLine = rawLine,
            )
        synchronized(this) {
            val next = _entries.value + line
            _entries.value = if (next.size > maxEntries) next.takeLast(maxEntries) else next
        }
    }

    fun clear() {
        synchronized(this) { _entries.value = emptyList() }
    }
}

internal fun classificationLabel(event: BarcodeEvent): String =
    when (event) {
        is BarcodeEvent.ProductBarcode -> "Товар (штрихкод)"
        is BarcodeEvent.EmployeeKey -> "Ключ сотрудника"
        is BarcodeEvent.RegistrationKey -> "Регистрация"
        is BarcodeEvent.ClientLoyaltyCard -> "Карта клиента (подписка)"
        is BarcodeEvent.UnknownBarcode -> "Неизвестный формат"
    }
