package com.viwa.android.data.payment.aqsi

/** USB Arcus2 terminal state for diagnostics UI. */
enum class UsbPaymentStatus {
    IDLE,
    CONNECTING,
    PROCESSING,
    WAITING_FOR_CARD,
    WAITING_FOR_PIN,
    AUTHORIZING,
    FINALIZING,
    CANCELLING,
    SUCCESS,
    FAILURE,
    TIMEOUT,
    CANCELLED,
    WARNING,
}
