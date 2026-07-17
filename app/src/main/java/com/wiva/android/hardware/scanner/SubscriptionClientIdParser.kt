package com.wiva.android.hardware.scanner

/**
 * Извлекает UUID клиента подписки из сырой строки сканера.
 * Формат.
 */
object SubscriptionClientIdParser {
    fun fromScannerRawLine(raw: String): String? = ScannerProtocol.extractSubscriptionClientId(raw)
}
