package com.wiva.android.data.remote.telemetry.mvp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class SerialAlreadyBoundException(
    val serialNumber: String,
    message: String = "Serial $serialNumber already bound to another installation",
) : Exception(message)

class MissingEnrollmentKeyException(
    message: String =
        "MVP enrollment key не задан. " +
            "Добавьте telemetry.enrollmentKey в local.properties " +
            "или WIVA_TELEMETRY_ENROLLMENT_KEY в окружение и пересоберите APK.",
) : Exception(message)

class MvpTelemetryApiClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val enrollmentKeyProvider: () -> String,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun reserveSerial(
        baseUrl: String,
        installationId: String?,
    ): Result<ReserveSerialResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireEnrollmentKey()
                val body =
                    json.encodeToString(
                        ReserveSerialRequestDto(
                            installationId = installationId?.takeIf { it.isNotBlank() },
                        ),
                    )
                val request =
                    Request.Builder()
                        .url("${baseUrl.trimEnd('/')}/api/v1/machines/serials/reserve")
                        .post(body.toRequestBody(jsonMediaType))
                        .header("Content-Type", "application/json")
                        .header("X-Enrollment-Key", enrollmentKeyProvider())
                        .build()
                executeJson(request, ReserveSerialResponseDto.serializer())
            }
        }

    suspend fun enroll(
        baseUrl: String,
        requestBody: EnrollRequestDto,
    ): Result<EnrollResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireEnrollmentKey()
                val body = json.encodeToString(EnrollRequestDto.serializer(), requestBody)
                val request =
                    Request.Builder()
                        .url("${baseUrl.trimEnd('/')}/api/v1/machines/enroll")
                        .post(body.toRequestBody(jsonMediaType))
                        .header("Content-Type", "application/json")
                        .header("X-Enrollment-Key", enrollmentKeyProvider())
                        .build()
                executeEnroll(request, requestBody.serialNumber)
            }
        }

    private fun requireEnrollmentKey() {
        if (enrollmentKeyProvider().trim().isBlank()) {
            throw MissingEnrollmentKeyException()
        }
    }

    private fun <T> executeJson(
        request: Request,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        httpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Timber.w("MvpTelemetry API ${response.code}: $text")
                error("HTTP ${response.code}: $text")
            }
            return json.decodeFromString(deserializer, text)
        }
    }

    private fun executeEnroll(request: Request, serialNumber: String): EnrollResponseDto {
        httpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 409) {
                when (EnrollConflictCodeParser.parseCode(json, text)) {
                    "SERIAL_ALREADY_BOUND" -> throw SerialAlreadyBoundException(serialNumber)
                    else -> error("HTTP 409: $text")
                }
            }
            if (!response.isSuccessful) {
                Timber.w("MvpTelemetry enroll HTTP ${response.code}: $text")
                error("HTTP ${response.code}: $text")
            }
            return json.decodeFromString(EnrollResponseDto.serializer(), text)
        }
    }
}
