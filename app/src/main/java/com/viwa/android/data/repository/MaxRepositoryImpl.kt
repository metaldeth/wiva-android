package com.viwa.android.data.repository

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.remote.max.MaxApiService
import com.viwa.android.data.remote.max.MaxVerifyAgeResponse
import com.viwa.android.domain.model.AgeVerificationResult
import com.viwa.android.domain.model.MaxSettings
import com.viwa.android.domain.repository.MaxRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun tryMigrateLegacy(jsonStr: String, json: Json): MaxSettings? {
    return runCatching {
        val element = json.parseToJsonElement(jsonStr).jsonObject
        val apiKey = element["apiKey"]?.jsonPrimitive?.content ?: return@runCatching null
        if (apiKey.isNotEmpty()) MaxSettings(extApiToken = apiKey) else null
    }.getOrNull()
}

internal fun parseStoredMaxSettingsJson(jsonStr: String, json: Json): MaxSettings {
    return runCatching {
        val element = json.parseToJsonElement(jsonStr).jsonObject
        val hasLegacyKey = element.containsKey("apiKey") && !element.containsKey("extApiToken")
        if (hasLegacyKey) {
            tryMigrateLegacy(jsonStr, json) ?: json.decodeFromString(jsonStr)
        } else {
            json.decodeFromString(jsonStr)
        }
    }.getOrElse {
        tryMigrateLegacy(jsonStr, json) ?: MaxSettings()
    }
}

class MaxRepositoryImpl
@Inject
constructor(
    private val api: MaxApiService,
    private val configRepository: ConfigRepository,
) : MaxRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override suspend fun verifyAge(sessionId: String): Result<AgeVerificationResult> =
        runCatching {
            val settings = getSettings()
            if (settings.extApiToken.isEmpty()) {
                return Result.failure(IllegalStateException("MAX extApiToken не настроен"))
            }
            val response =
                api.verifyAge(
                    authHeader = settings.extApiToken,
                    sessionId = sessionId.trim(),
                    verificationDetails = settings.verificationDetailsEnabled,
                )
            if (!response.isSuccessful) {
                return Result.failure(Exception("MAX API error: ${response.code()}"))
            }
            val body = response.body() ?: return Result.failure(Exception("Пустой ответ от MAX"))
            mapResponseToResult(body)
        }

    private fun mapResponseToResult(response: MaxVerifyAgeResponse): AgeVerificationResult {
        if (response.status == "success") {
            val verificationStatusNorm = response.data?.verificationStatus?.lowercase() ?: ""
            val adultStatus = response.data?.verificationDetails?.adult?.status
            return when {
                verificationStatusNorm == "cancelled" || verificationStatusNorm == "canceled" ->
                    AgeVerificationResult.Cancelled
                verificationStatusNorm == "timeout" || verificationStatusNorm == "expired" ->
                    AgeVerificationResult.Timeout
                adultStatus == true -> AgeVerificationResult.Approved
                adultStatus == false -> AgeVerificationResult.Rejected("Возраст не подтверждён")
                response.data?.verificationStatus == "completed" -> AgeVerificationResult.Approved
                else -> AgeVerificationResult.Rejected("Возраст не подтверждён")
            }
        }
        val reason = response.error?.message ?: "Не удалось проверить возраст"
        return AgeVerificationResult.Rejected(reason)
    }

    override suspend fun getSettings(): MaxSettings {
        val jsonStr = configRepository.getJson(JsonStoreKeys.MAX_SETTINGS) ?: return MaxSettings()
        return parseStoredMaxSettingsJson(jsonStr, json)
    }

    override suspend fun updateSettings(settings: MaxSettings) {
        configRepository.setJson(JsonStoreKeys.MAX_SETTINGS, json.encodeToString(MaxSettings.serializer(), settings))
    }
}
