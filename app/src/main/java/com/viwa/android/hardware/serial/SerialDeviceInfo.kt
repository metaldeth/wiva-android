package com.viwa.android.hardware.serial

/** Метаданные serial/USB устройства для экрана «Устройства». */
data class SerialDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val driverType: String?,
)

fun PaymentSerialDeviceInfo.toSerialDeviceInfo(): SerialDeviceInfo =
    SerialDeviceInfo(
        deviceName = deviceName,
        vendorId = vendorId,
        productId = productId,
        driverType = driverType,
    )
