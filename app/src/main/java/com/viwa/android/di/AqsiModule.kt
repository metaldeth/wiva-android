package com.viwa.android.di

import com.viwa.android.data.payment.aqsi.Arcus2Client
import com.viwa.android.data.payment.aqsi.Arcus2TerminalClient
import com.viwa.android.data.payment.aqsi.AqsiLastOperationSnapshotHolder
import com.viwa.android.data.payment.aqsi.AqsiRepositoryImpl
import com.viwa.android.data.repository.CardPaymentMockModeRepositoryImpl
import com.viwa.android.data.repository.CardPaymentMethodRepositoryImpl
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import com.viwa.android.services.payment.CardPaymentOrchestrator
import com.viwa.android.services.payment.CardPaymentEventLogger
import com.viwa.android.services.payment.PaymentTerminalService
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
