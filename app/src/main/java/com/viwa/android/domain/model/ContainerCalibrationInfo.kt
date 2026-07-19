package com.viwa.android.domain.model

/** Контейнер для экрана калибровки сиропа (merge-конфиг). */
data class ContainerCalibrationInfo(
    val containerNumber: Int,
    val catalogTitle: String,
    val conversionFactor: Double,
 /** Целевая порция продукта, мл (dosage.product), если не задана вручную при сохранении. */
    val defaultProductMl: Double,
)
