package com.viwa.android.hardware.serial

/** USB serial device metadata for payment terminal discovery. */
data class PaymentSerialDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val driverType: String?,
)
