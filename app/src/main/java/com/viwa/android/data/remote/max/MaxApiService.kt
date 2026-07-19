package com.viwa.android.data.remote.max

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MaxApiService {
    @GET("/v2/business/pos/age-verification")
    suspend fun verifyAge(
        @Header("Authorization") authHeader: String,
        @Query("session_id") sessionId: String,
        @Query("verification_details") verificationDetails: Boolean = false,
    ): Response<MaxVerifyAgeResponse>
}

@Serializable
data class MaxVerifyAgeResponse(
    val status: String,
    val data: MaxVerifyAgeData? = null,
    val error: MaxVerifyAgeError? = null,
)

@Serializable
data class MaxVerifyAgeData(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("verification_status") val verificationStatus: String = "",
    @SerialName("verification_details") val verificationDetails: MaxVerifyAgeDetails? = null,
)

@Serializable
data class MaxVerifyAgeDetails(
    val adult: MaxAdultStatus? = null,
)

@Serializable
data class MaxAdultStatus(
    val status: Boolean = false,
)

@Serializable
data class MaxVerifyAgeError(
    val code: String = "",
    val message: String = "",
    val details: String = "",
)
