package com.viwa.android.data.remote.telemetry.mvp

import com.viwa.android.data.local.sales.PendingSale
import com.viwa.android.data.local.sales.SalesOutboxStore
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TelemetrySalesSyncCoordinator
@Inject
constructor(
    private val outboxStore: SalesOutboxStore,
    private val wsManager: MvpTelemetryWebSocketManager,
) {
    suspend fun enqueueAndTrySend(sale: PendingSale) {
        outboxStore.enqueue(sale)
        trySendSale(sale)
    }

    suspend fun onWebSocketHello() {
        flushPending()
    }

    suspend fun flushPending() {
        outboxStore.listPending().forEach { sale ->
            trySendSale(sale)
        }
    }

    private suspend fun trySendSale(sale: PendingSale) {
        val payload = TelemetrySalesMessageCodec.encodeSaleReportPayload(sale)
        wsManager
            .sendEnvelope(type = "sale.report", payload = payload)
            .onSuccess {
                outboxStore.markSent(sale.saleId)
                Timber.i("TelemetrySalesSync: sale.report sent saleId=${sale.saleId}")
            }.onFailure { error ->
                outboxStore.bumpAttempt(sale.saleId)
                Timber.w(error, "TelemetrySalesSync: sale.report failed saleId=${sale.saleId}")
            }
    }
}
