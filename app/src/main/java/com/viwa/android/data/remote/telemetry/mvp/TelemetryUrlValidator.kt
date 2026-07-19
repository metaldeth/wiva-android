package com.viwa.android.data.remote.telemetry.mvp



import com.viwa.android.domain.model.TelemetryConfig

import java.net.URI



/** Strict HTTPS origin validation and trust check against persisted telemetry apiUrl. */

object TelemetryUrlValidator {

    const val MAX_ORIGIN_LENGTH = 256

    private const val DEFAULT_HTTPS_PORT = 443



    sealed interface Result {

        data class Valid(val normalizedOrigin: String) : Result



        data class Invalid(val reason: String) : Result

    }



    fun validateStrict(input: String): Result {

        val trimmed = input.trim()

        if (trimmed.isEmpty()) {

            return Result.Invalid("Укажите HTTPS URL API")

        }

        if (trimmed.length > MAX_ORIGIN_LENGTH) {

            return Result.Invalid("URL API слишком длинный")

        }



        val uri =

            runCatching { URI(trimmed) }.getOrElse {

                return Result.Invalid("Некорректный URL API")

            }



        when (uri.scheme?.lowercase()) {

            "https" -> Unit

            "http" -> return Result.Invalid("apiUrl должен быть HTTPS, не HTTP")

            "file" -> return Result.Invalid("apiUrl не может быть file://")

            null -> return Result.Invalid("apiUrl должен начинаться с https://")

            else -> return Result.Invalid("apiUrl: только HTTPS")

        }



        if (!uri.userInfo.isNullOrBlank()) {

            return Result.Invalid("apiUrl не должен содержать логин или пароль")

        }

        if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) {

            return Result.Invalid("apiUrl не должен содержать query или fragment")

        }



        val path = uri.path.orEmpty()

        if (path.isNotEmpty() && path != "/") {

            return Result.Invalid("apiUrl должен быть origin без path")

        }



        val host = uri.host?.trim().orEmpty()

        if (host.isEmpty()) {

            return Result.Invalid("Некорректный host в apiUrl")

        }



        return Result.Valid(buildOrigin(host, effectivePort(uri)))

    }



    /** Accept QR apiUrl only when host+port matches persisted trusted apiUrl (or default). */

    fun validateTrustedCandidate(candidate: String, persistedApiUrl: String): Result {

        val candidateResult = validateStrict(candidate)

        if (candidateResult is Result.Invalid) {

            return candidateResult

        }



        val trustedResult = validateStrict(persistedApiUrl.trim().ifBlank { TelemetryConfig.DEFAULT_API_URL })

        if (trustedResult is Result.Invalid) {
            val defaultResult = validateStrict(TelemetryConfig.DEFAULT_API_URL)
            return if (candidateResult is Result.Valid && defaultResult is Result.Valid) {
                compareOrigins(candidateResult, defaultResult)
            } else {
                defaultResult
            }
        }

        return if (candidateResult is Result.Valid && trustedResult is Result.Valid) {
            compareOrigins(candidateResult, trustedResult)
        } else {
            candidateResult
        }

    }



    private fun compareOrigins(candidate: Result.Valid, trusted: Result.Valid): Result {

        if (candidate.normalizedOrigin != trusted.normalizedOrigin) {

            return Result.Invalid(

                "QR указывает другой сервер (${candidate.normalizedOrigin}). " +

                    "Сначала измените адрес API вручную, сохраните, затем повторите сканирование.",

            )

        }

        return candidate

    }



    private fun effectivePort(uri: URI): Int =

        if (uri.port != -1) {

            uri.port

        } else {

            DEFAULT_HTTPS_PORT

        }



    private fun buildOrigin(host: String, port: Int): String =

        if (port == DEFAULT_HTTPS_PORT) {

            "https://$host"

        } else {

            "https://$host:$port"

        }

}

