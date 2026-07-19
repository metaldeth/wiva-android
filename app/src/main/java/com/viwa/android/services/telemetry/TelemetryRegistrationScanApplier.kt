package com.viwa.android.services.telemetry

import com.viwa.android.data.remote.telemetry.mvp.TelemetryUrlValidator

/** Pure apply logic for telemetry registration scan events (QR / REG key). */
internal object TelemetryRegistrationScanApplier {
    data class Result(
        val regKey: String,
        val serial: String? = null,
        val apiUrl: String? = null,
        val qrBanner: String,
        val urlWarning: String? = null,
    )

    fun apply(
        event: TelemetryRegistrationScanUiEvent,
        currentApiUrl: String,
    ): Result {
        val qrBanner =
            if (event.apiUrl != null) {
                "QR регистрации считан"
            } else {
                "REG-ключ считан"
            }

        var apiUrlToSet: String? = null
        var urlWarning: String? = null

        event.apiUrl?.let { candidate ->
            when (val validated = TelemetryUrlValidator.validateTrustedCandidate(candidate, currentApiUrl)) {
                is TelemetryUrlValidator.Result.Valid -> apiUrlToSet = validated.normalizedOrigin
                is TelemetryUrlValidator.Result.Invalid -> urlWarning = validated.reason
            }
        }

        return Result(
            regKey = event.registrationKey,
            serial = event.serialNumber,
            apiUrl = apiUrlToSet,
            qrBanner = qrBanner,
            urlWarning = urlWarning,
        )
    }

    /** Full JSON QR with serial + REG key → auto-register in ServiceViewModel (rebind still manual). */
    fun shouldAutoRegister(result: Result): Boolean =
        result.regKey.isNotBlank() && !result.serial.isNullOrBlank()
}
