package com.wiva.android.di

import com.wiva.android.data.remote.max.MaxApiService
import com.wiva.android.data.network.NetworkTrafficInterceptor
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.data.repository.MaxRepositoryImpl
import com.wiva.android.data.repository.NanoKassaRepositoryImpl
import com.wiva.android.data.repository.SBPRepositoryImpl
import com.wiva.android.domain.repository.MaxRepository
import com.wiva.android.domain.repository.NanoKassaRepository
import com.wiva.android.domain.repository.SBPRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object IntegrationsModule {
    @Provides
    @Singleton
    @Named("max")
    fun provideMaxOkHttpClient(networkTrafficInterceptor: NetworkTrafficInterceptor): OkHttpClient {
        val tlsSpec =
            ConnectionSpec
                .Builder(ConnectionSpec.COMPATIBLE_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                .build()
        return OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionSpecs(listOf(tlsSpec))
            .addInterceptor(networkTrafficInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("max")
    fun provideMaxRetrofit(@Named("max") okHttpClient: OkHttpClient): Retrofit {
        val json =
            Json {
                ignoreUnknownKeys = true
            }
        return Retrofit.Builder()
            .baseUrl("https://ext-api.max.ru/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideMaxApiService(@Named("max") retrofit: Retrofit): MaxApiService = retrofit.create(MaxApiService::class.java)

    @Provides
    @Singleton
    fun provideMaxRepository(
        api: MaxApiService,
        configRepository: ConfigRepository,
    ): MaxRepository = MaxRepositoryImpl(api, configRepository)

    @Provides
    @Singleton
    fun provideSbpRepository(
        okHttpClient: OkHttpClient,
        configRepository: ConfigRepository,
    ): SBPRepository = SBPRepositoryImpl(okHttpClient, configRepository)

    @Provides
    @Singleton
    fun provideNanoKassaRepository(
        okHttpClient: OkHttpClient,
        configRepository: ConfigRepository,
    ): NanoKassaRepository = NanoKassaRepositoryImpl(okHttpClient, configRepository)
}
