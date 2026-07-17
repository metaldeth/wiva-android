package com.wiva.android.data.remote.telemetry

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface TelemetryApiService {
    @POST
    suspend fun registerMachine(
        @Url url: String,
        @Body body: MachineRegistrationRequestDto,
    ): Response<RegistrationResponseDto>
}
