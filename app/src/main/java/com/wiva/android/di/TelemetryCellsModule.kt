package com.wiva.android.di

import com.wiva.android.data.repository.TelemetryCellsRepositoryImpl
import com.wiva.android.domain.repository.TelemetryCellsRepository
import com.wiva.android.domain.telemetry.DefaultPhysicalCellSchemaProvider
import com.wiva.android.domain.telemetry.PhysicalCellSchemaProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryCellsModule {
    @Binds
    @Singleton
    abstract fun bindTelemetryCellsRepository(impl: TelemetryCellsRepositoryImpl): TelemetryCellsRepository

    @Binds
    @Singleton
    abstract fun bindPhysicalCellSchemaProvider(impl: DefaultPhysicalCellSchemaProvider): PhysicalCellSchemaProvider
}
