package com.wiva.android.data.remote.telemetry.mvp

/** REG-key validation: `REG-` + 12 uppercase Crockford Base32 chars (server contract). */
object RegistrationKeyUtils {
    /** Crockford: digits 0-9, excludes ambiguous I, L, O, U. */
    private const val CROCKFORD_BODY = "0-9A-HJKMNP-TV-Z"

    private val FULL_KEY_PATTERN = Regex("^REG-[$CROCKFORD_BODY]{12}$")
    private val BODY_PATTERN = Regex("^[$CROCKFORD_BODY]{12}$")

    fun normalize(input: String): String {
        val trimmed = input.trim().uppercase()
        return when {
            trimmed.startsWith("REG-") -> trimmed
            trimmed.startsWith("REG:") -> "REG-${trimmed.removePrefix("REG:")}"
            BODY_PATTERN.matches(trimmed) -> "REG-$trimmed"
            else -> trimmed
        }
    }

    fun isValid(input: String): Boolean = FULL_KEY_PATTERN.matches(normalize(input))

    fun validationMessage(input: String): String? =
        when {
            input.isBlank() -> "Введите REG-ключ"
            !isValid(input) ->
                "Формат: REG- и 12 символов Crockford (0-9, A-H, J-N, P-T, V-Z; без I, L, O, U)"
            else -> null
        }
}
