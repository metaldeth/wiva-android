package com.wiva.android.data.payment.aqsi

import com.wiva.android.domain.model.AqsiPaymentResult
import java.nio.charset.Charset

/**
 * Разбор текстового JPAY по документации KB (STORERC, OK/ER).
 * Коды одобрения/отказа — таблица 5 (Universal EMV POS); в payload не должно быть PAN/трека.
 */
internal object JpayPaymentOutcomeParser {

 /** Единый публичный код для строки протокола `ER` (без PAN, для UI). */
    const val DECLINED_PUBLIC_CODE_ER: String = "er"

    private val jpayCharset: Charset by lazy { Charset.forName("windows-1251") }

    fun decodeInnerPayload(bytes: ByteArray): String =
        String(bytes, jpayCharset).trim()

 /**
 * Интерпретация строки из поля DATA после BinLen (ответ ридера или финальный STORERC).
 */
    fun interpretPaymentLine(text: String): JpayInterpretation =
        when {
            text.equals("OK", ignoreCase = true) || text.startsWith("OK:", ignoreCase = true) ->
                JpayInterpretation.IntermediateOk

            text.equals("ER", ignoreCase = true) ->
                JpayInterpretation.OperationEr

            text.startsWith("STORERC:", ignoreCase = true) -> {
                val rawCode = text.substringAfter(":").trim().filterNot { it.isWhitespace() }
                if (isApprovedResponseCode(rawCode)) {
                    JpayInterpretation.TerminalApproved
                } else {
                    val safe = rawCode.filter { it.isDigit() }.take(6).ifEmpty { "unknown" }
                    JpayInterpretation.TerminalDeclined(safe)
                }
            }

            else -> JpayInterpretation.OtherCommand(text.take(48))
        }

    fun toPaymentResult(interpretation: JpayInterpretation): Result<AqsiPaymentResult>? =
        when (interpretation) {
            JpayInterpretation.IntermediateOk -> null
            JpayInterpretation.OperationEr ->
 // ER — ответ ридера «ошибка/отказ» (KB OK/ER), для UC-2 мапится как отказ.
                Result.success(AqsiPaymentResult.Declined(publicCode = DECLINED_PUBLIC_CODE_ER))

            JpayInterpretation.TerminalApproved ->
                Result.success(AqsiPaymentResult.Approved)

            is JpayInterpretation.TerminalDeclined ->
                Result.success(AqsiPaymentResult.Declined(publicCode = interpretation.code))

            is JpayInterpretation.OtherCommand -> null
        }

 /** Коды вида 00 / 000 трактуются как одобрено (KB таблица 5). */
    private fun isApprovedResponseCode(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return false
        val value = digits.toIntOrNull() ?: return false
        return value == 0
    }
}

internal sealed interface JpayInterpretation {
    data object IntermediateOk : JpayInterpretation

    data object OperationEr : JpayInterpretation

    data object TerminalApproved : JpayInterpretation

    data class TerminalDeclined(val code: String) : JpayInterpretation

 /** Другая команда ридера — нужен ответ OK на хосте (обрабатывает клиент). */
    data class OtherCommand(val preview: String) : JpayInterpretation
}
