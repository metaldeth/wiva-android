package com.viwa.android.domain.model

import kotlinx.serialization.Serializable

/** Элемент локального каталога продуктов из snapshot downlink. */
@Serializable
data class TelemetryProduct(
    val uuid: String,
    val name: String,
    val tasteMediaKey: String,
)
