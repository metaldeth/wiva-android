package com.wiva.android.di

import com.wiva.android.data.local.security.EncryptedMachineSecretStore
import com.wiva.android.data.local.security.MachineSecretStore
import com.wiva.android.data.remote.telemetry.WivaTelemetryEventBus
import com.wiva.android.data.remote.telemetry.WivaTelemetryWebSocketManager
import com.wiva.android.data.network.NetworkTrafficLogger
import com.wiva.android.data.remote.telemetry.TelemetryApiService
import com.wiva.android.data.remote.telemetry.mvp.EpochMillisClock
import com.wiva.android.data.remote.telemetry.mvp.MvpTelemetryApiClient
import com.wiva.android.data.remote.telemetry.mvp.SystemEpochMillisClock
import com.wiva.android.domain.model.TelemetryConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides
    @Singleton
    @Named("telemetry")
    fun provideTelemetryRetrofit(
        @Named("telemetryHttp") okHttpClient: OkHttpClient,
    ): Retrofit {
        val json =
            Json {
                ignoreUnknownKeys = true
            }
        return Retrofit.Builder()
            .baseUrl("${TelemetryConfig.DEFAULT_API_URL}/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

 /**
 * Для legacy-Android (API 25) стабилизируем TLS handshake:
 * отключаем HTTP/2 в telemetry-канале и оставляем HTTP/1.1.
 */
    @Provides
    @Singleton
    @Named("telemetryHttp")
    fun provideTelemetryOkHttpClient(baseClient: OkHttpClient): OkHttpClient =
        baseClient
            .newBuilder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

    @Provides
    @Singleton
    fun provideTelemetryApiService(@Named("telemetry") retrofit: Retrofit): TelemetryApiService =
        retrofit.create(TelemetryApiService::class.java)

    @Provides
    @Singleton
    fun provideMachineSecretStore(store: EncryptedMachineSecretStore): MachineSecretStore = store

    @Provides
    @Singleton
    fun provideWivaTelemetryWebSocketManager(
        eventBus: WivaTelemetryEventBus,
        @AppIoScope appScope: CoroutineScope,
        networkTrafficLogger: NetworkTrafficLogger,
    ): WivaTelemetryWebSocketManager =
        WivaTelemetryWebSocketManager(
            eventBus,
            appScope,
            networkTrafficLogger,
        )

    @Provides
    @Singleton
    fun provideEpochMillisClock(): EpochMillisClock = SystemEpochMillisClock()

    @Provides
    @Singleton
    fun provideMvpTelemetryApiClient(
        @Named("telemetryHttp") okHttpClient: OkHttpClient,
    ): MvpTelemetryApiClient {
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        return MvpTelemetryApiClient(
            httpClient = okHttpClient,
            json = json,
            enrollmentKeyProvider = { com.wiva.android.BuildConfig.TELEMETRY_ENROLLMENT_KEY },
        )
    }
}
