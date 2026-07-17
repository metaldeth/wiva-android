package com.wiva.android.data.remote.telemetry

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/** Keycloak client_credentials. */
object WivaTelemetryAuth {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    suspend fun fetchAccessToken(
        client: OkHttpClient,
        keycloakBase: String,
        realm: String,
        clientSecret: String,
        clientId: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val url = "$keycloakBase/realms/$realm/protocol/openid-connect/token"
            val result =
                runCatching {
                val body =
                    FormBody
                        .Builder()
                        .add("grant_type", "client_credentials")
                        .add("scope", "profile")
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .build()
                    val request =
                        Request.Builder()
                            .url(url)
                            .post(body)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .build()
                    client.newCall(request).execute().use { response ->
                        val text = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            Timber.w("Keycloak token HTTP ${response.code}: $text")
                            error("Keycloak ${response.code}: $text")
                        }
                        val token =
                            json.parseToJsonElement(text).jsonObject["access_token"]?.jsonPrimitive?.content
                                ?: error("No access_token in response")
                        token
                    }
                }
            result.onFailure { e ->
                Timber.w(
                    e,
                    "Keycloak client_credentials failed url=%s realm=%s client_id=%s",
                    url,
                    realm,
                    clientId,
                )
            }
            result
        }
}
