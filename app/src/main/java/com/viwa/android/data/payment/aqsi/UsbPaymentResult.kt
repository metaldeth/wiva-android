package com.viwa.android.data.payment.aqsi

/** Result of one USB Arcus2 payment attempt. */
sealed interface UsbPaymentResult {
    data class Success(val transactionId: String, val amountKopecks: Int) : UsbPaymentResult

    data class Failure(val errorCode: String, val message: String) : UsbPaymentResult

    data object Cancelled : UsbPaymentResult

    data object Timeout : UsbPaymentResult
}
