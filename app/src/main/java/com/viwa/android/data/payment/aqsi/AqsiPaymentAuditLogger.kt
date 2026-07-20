package com.viwa.android.data.payment.aqsi

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** AQSI audit lines — Timber only (no separate payment audit DB in Viwa). */
@Singleton
class AqsiPaymentAuditLogger
@Inject
constructor() {
    fun log(
        level: String,
        tag: String,
        message: String,
    ) {
        when (level) {
            "ERROR" -> Timber.tag(tag).e(message)
            "WARN" -> Timber.tag(tag).w(message)
            else -> Timber.tag(tag).d(message)
        }
    }

    fun log(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        when (level) {
            "ERROR" -> Timber.tag(tag).e(throwable, message)
            "WARN" -> Timber.tag(tag).w(throwable, message)
            else -> Timber.tag(tag).d(throwable, message)
        }
    }
}
