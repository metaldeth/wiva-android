package com.wiva.android.data.remote.telemetry.mvp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Parsed telemetry registration QR v1 or raw REG-key scan. */
data class TelemetryRegistrationScan(
    val registrationKey: String,
    val serialNumber: String? = null,
    val apiUrl: String? = null,
)

sealed class TelemetryRegistrationQrParseResult {
    data class Success(val scan: TelemetryRegistrationScan) : TelemetryRegistrationQrParseResult()

    data class Invalid(val reason: String) : TelemetryRegistrationQrParseResult()
}

@Serializable
private data class TelemetryRegistrationQrV1Dto(
    val type: String,
    val version: Int,
    val registrationKey: String,
    val serialNumber: String? = null,
    val apiUrl: String? = null,
)

object TelemetryRegistrationQrParser {
    private const val EXPECTED_TYPE = "WIVA_TELEMETRY_REGISTRATION"
    private const val EXPECTED_VERSION = 1

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

    fun parse(raw: String): TelemetryRegistrationQrParseResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return TelemetryRegistrationQrParseResult.Invalid("Пустой QR")

        if (trimmed.startsWith("{")) {
            return parseJsonV1(trimmed)
        }

        val normalizedKey = RegistrationKeyUtils.normalize(trimmed)
        if (RegistrationKeyUtils.isValid(normalizedKey)) {
            return TelemetryRegistrationQrParseResult.Success(
                TelemetryRegistrationScan(registrationKey = normalizedKey),
            )
        }

        return TelemetryRegistrationQrParseResult.Invalid("Неизвестный формат QR/REG")
    }

    private fun parseJsonV1(text: String): TelemetryRegistrationQrParseResult =
        runCatching {
            val dto = json.decodeFromString<TelemetryRegistrationQrV1Dto>(text)
            if (dto.type != EXPECTED_TYPE) {
                return TelemetryRegistrationQrParseResult.Invalid("Неверный type QR")
            }
            if (dto.version != EXPECTED_VERSION) {
                return TelemetryRegistrationQrParseResult.Invalid("Неподдерживаемая версия QR")
            }
            val key = RegistrationKeyUtils.normalize(dto.registrationKey)
            if (!RegistrationKeyUtils.isValid(key)) {
                return TelemetryRegistrationQrParseResult.Invalid("Неверный registrationKey в QR")
            }
            val serial =
                dto.serialNumber
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { SerialNumberUtils.normalize(it) }
            val apiUrl =
                dto.apiUrl
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { rawUrl ->
                        when (val validated = TelemetryUrlValidator.validateStrict(rawUrl)) {
                            is TelemetryUrlValidator.Result.Valid -> validated.normalizedOrigin
                            is TelemetryUrlValidator.Result.Invalid ->
                                return TelemetryRegistrationQrParseResult.Invalid(validated.reason)
                        }
                    }
            TelemetryRegistrationQrParseResult.Success(
                TelemetryRegistrationScan(
                    registrationKey = key,
                    serialNumber = serial,
                    apiUrl = apiUrl,
                ),
            )
        }.getOrElse {
            TelemetryRegistrationQrParseResult.Invalid("Ошибка разбора JSON QR")
        }
}
