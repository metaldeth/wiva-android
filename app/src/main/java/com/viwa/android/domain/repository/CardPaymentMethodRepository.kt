package com.viwa.android.domain.repository

import com.viwa.android.domain.model.CardPaymentMethod

/**
 * Выбранный способ оплаты картой (см. [CardPaymentMethod]). В персистентности хранится строкой
 * `"PAX"` / `"AQSI"` ([CardPaymentMethod.STORAGE_PAX], [CardPaymentMethod.STORAGE_AQSI]).
 * При отсутствии записи, пустой/пробельной строке или неизвестном значении [getSelected] возвращает [CardPaymentMethod.Pax].
 */
interface CardPaymentMethodRepository {
    suspend fun getSelected(): CardPaymentMethod

    suspend fun setSelected(method: CardPaymentMethod)
}
