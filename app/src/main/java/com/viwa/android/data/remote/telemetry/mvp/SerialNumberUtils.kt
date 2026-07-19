package com.viwa.android.data.remote.telemetry.mvp

/** Валидация и нормализация серийного номера автомата (формат VIWA-000001). */
object SerialNumberUtils {
    private val SERIAL_PATTERN = Regex("^VIWA-\\d{6}$", RegexOption.IGNORE_CASE)

    fun normalize(input: String): String {
        val trimmed = input.trim().uppercase()
        if (trimmed.isEmpty()) return trimmed
        val compact = trimmed.replace(Regex("[\\s_]"), "-")
        val match = Regex("VIWA-?(\\d+)").find(compact) ?: return compact
        val digits = match.groupValues[1].padStart(6, '0')
        return "VIWA-$digits"
    }

    fun isValid(input: String): Boolean = SERIAL_PATTERN.matches(normalize(input))

    fun validationMessage(input: String): String? =
        when {
            input.isBlank() -> "Введите серийный номер"
            !isValid(input) -> "Формат: VIWA-000001 (6 цифр)"
            else -> null
        }
}
