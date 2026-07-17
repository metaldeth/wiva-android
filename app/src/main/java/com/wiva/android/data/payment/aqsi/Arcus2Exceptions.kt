package com.wiva.android.data.payment.aqsi

/** Ошибка разбора кадра Arcus2 / BinLen без чувствительных деталей в сообщении. */
class Arcus2ProtocolException(message: String) : Exception(message)

/** Сетевая / транспортная ошибка aQsi (для [Result.failure]). */
class AqsiTransportException(message: String, cause: Throwable? = null) : Exception(message, cause)
