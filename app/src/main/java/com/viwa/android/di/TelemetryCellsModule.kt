package com.viwa.android.di

import com.viwa.android.data.repository.TelemetryCellsRepositoryImpl
import com.viwa.android.domain.repository.TelemetryCellsRepository
import com.viwa.android.domain.telemetry.DefaultPhysicalCellSchemaProvider
import com.viwa.android.domain.telemetry.PhysicalCellSchemaProvider
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
