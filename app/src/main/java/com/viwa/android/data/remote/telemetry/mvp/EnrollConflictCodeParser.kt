package com.viwa.android.data.remote.telemetry.mvp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Парсинг code из flat и Nest-nested 409 тел enroll/reserve. */
object EnrollConflictCodeParser {
    @Serializable
    private data class FlatErrorBodyDto(
        val code: String? = null,
    )

    @Serializable
    private data class NestErrorBodyDto(
        val code: String? = null,
        val message: JsonElement? = null,
    )

    fun parseCode(json: Json, text: String): String? {
        runCatching { json.decodeFromString(FlatErrorBodyDto.serializer(), text) }
            .getOrNull()
            ?.code
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        runCatching { json.decodeFromString(NestErrorBodyDto.serializer(), text) }
            .getOrNull()
            ?.let { body ->
                body.code?.takeIf { it.isNotBlank() }?.let { return it }
                (body.message as? JsonObject)
                    ?.get("code")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }

        if (text.contains("SERIAL_ALREADY_BOUND")) {
            return "SERIAL_ALREADY_BOUND"
        }
        if (text.contains("REBIND_NOT_ALLOWED")) {
            return "REBIND_NOT_ALLOWED"
        }
        return null
    }
}
