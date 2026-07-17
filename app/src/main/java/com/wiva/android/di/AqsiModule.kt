package com.wiva.android.di

import com.wiva.android.data.payment.aqsi.Arcus2Client
import com.wiva.android.data.payment.aqsi.Arcus2TerminalClient
import com.wiva.android.data.payment.aqsi.AqsiLastOperationSnapshotHolder
import com.wiva.android.data.payment.aqsi.AqsiRepositoryImpl
import com.wiva.android.data.repository.CardPaymentMockModeRepositoryImpl
import com.wiva.android.data.repository.CardPaymentMethodRepositoryImpl
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.domain.repository.AqsiRepository
import com.wiva.android.domain.repository.CardPaymentMockModeRepository
import com.wiva.android.domain.repository.CardPaymentMethodRepository
import com.wiva.android.services.payment.CardPaymentOrchestrator
import com.wiva.android.services.payment.CardPaymentEventLogger
import com.wiva.android.services.payment.PaymentTerminalService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AqsiModule {

    @Provides
    @Singleton
    fun provideAqsiLastOperationSnapshotHolder(): AqsiLastOperationSnapshotHolder = AqsiLastOperationSnapshotHolder()

    @Provides
    @Singleton
    fun provideArcus2TerminalClient(): Arcus2TerminalClient = Arcus2Client()

    @Provides
    @Singleton
    fun provideAqsiRepository(
        configRepository: ConfigRepository,
        arcus2TerminalClient: Arcus2TerminalClient,
        aqsiLastOperationSnapshotHolder: AqsiLastOperationSnapshotHolder,
        paymentEventLogger: CardPaymentEventLogger,
    ): AqsiRepository =
        AqsiRepositoryImpl(
            configRepository = configRepository,
            arcus2 = arcus2TerminalClient,
            lastOperationSnapshotHolder = aqsiLastOperationSnapshotHolder,
            paymentEventLogger = paymentEventLogger,
        )

    @Provides
    @Singleton
    fun provideCardPaymentMethodRepository(
        configRepository: ConfigRepository,
    ): CardPaymentMethodRepository = CardPaymentMethodRepositoryImpl(configRepository)

    @Provides
    @Singleton
    fun provideCardPaymentMockModeRepository(
        configRepository: ConfigRepository,
    ): CardPaymentMockModeRepository = CardPaymentMockModeRepositoryImpl(configRepository)

    @Provides
    @Singleton
    fun provideCardPaymentOrchestrator(
        cardPaymentMethodRepository: CardPaymentMethodRepository,
        cardPaymentMockModeRepository: CardPaymentMockModeRepository,
        paymentTerminalService: PaymentTerminalService,
        aqsiRepository: AqsiRepository,
        paymentEventLogger: CardPaymentEventLogger,
    ): CardPaymentOrchestrator =
        CardPaymentOrchestrator(
            cardPaymentMethodRepository = cardPaymentMethodRepository,
            cardPaymentMockModeRepository = cardPaymentMockModeRepository,
            paymentTerminalService = paymentTerminalService,
            aqsiRepository = aqsiRepository,
            paymentEventLogger = paymentEventLogger,
        )
}
