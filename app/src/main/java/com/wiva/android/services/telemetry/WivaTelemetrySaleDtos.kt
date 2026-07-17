package com.wiva.android.services.telemetry

import kotlinx.serialization.Serializable

/**
 * Упрощённая строка продажи для [WivaTelemetryService.sendSaleImportTopic] — дальше маппится в [SaleImportItemJson].
 * [drinkId] — id ингредиента (`product.id`); [price] — каталожная цена выбранного объёма; при оплате [totalChargedRub] — факт списания.
 */
data class SaleImportItem(
    val drinkId: Int,
    val volume: Int,
    val price: Double,
    val payMethod: String?,
    val totalChargedRub: Double? = null,
)

/**ts` (отправка на WS). */
@Serializable
data class SaleImportWriteOffJson(
    val cellNumber: Int,
    val ingredientId: Int,
    val volume: Int,
    val cellType: String = "INGREDIENT",
    val unit: String = "G",
)

@Serializable
data class SaleImportPaymentJson(
    val price: Double,
    val method: String,
)

@Serializable
data class SaleImportItemJson(
    val dateSale: String,
    val orgId: Int,
    val machineId: Int,
    val promocodeId: Int? = null,
    val name: String,
    val volume: Int,
    val discountId: Int? = null,
    val price: Double,
    val totalPrice: Double,
    val unit: String = "ML",
    val writeOffs: List<SaleImportWriteOffJson>,
    val payments: List<SaleImportPaymentJson>,
)

@Serializable
data class SaleImportOutboundMessage(
    val type: String = "saleImportTopic",
    val clientId: String,
    val body: List<SaleImportItemJson>,
)
