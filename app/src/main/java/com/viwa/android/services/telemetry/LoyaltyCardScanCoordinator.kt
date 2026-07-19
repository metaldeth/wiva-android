package com.viwa.android.services.telemetry

import com.viwa.android.di.AppIoScope
import com.viwa.android.hardware.scanner.ViwaScannerTrafficLogger
import com.viwa.android.hardware.scanner.SubscriptionClientIdParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "LoyaltyCardScan"

/**
 * Глобальная подписка на строки сканера для карт `CLIENT_<uuid>`
 * [subscriptionModule] + [scannerEvents.onLoyaltyCardArrived]: без привязки к экрану
 * (меню напитков, экран «попробуй воду» / FreeDrinkOffer, и т.д.).
 */
@Singleton
class LoyaltyCardScanCoordinator
    @Inject
    constructor(
        private val scannerTrafficLogger: ViwaScannerTrafficLogger,
        private val telemetryService: ViwaTelemetryService,
        @AppIoScope private val appScope: CoroutineScope,
    ) {
        init {
            appScope.launch {
                var lastSeenId: Int? = null
                scannerTrafficLogger.entries.collect { entries ->
                    val last = entries.lastOrNull() ?: return@collect
                    if (lastSeenId == last.id) return@collect
                    lastSeenId = last.id
                    val clientUuid = SubscriptionClientIdParser.fromScannerRawLine(last.rawLine) ?: return@collect
                    Timber.tag(TAG).d("loyalty scan → telemetry (uuid=%s)", clientUuid)
                    telemetryService.onLoyaltyCardScanned(clientUuid)
                }
            }
        }
    }
