package com.viwa.android.data.payment.aqsi

/** Интерпретирует итоговые коды AQSI из STORERC и строк печатного чека. */
internal object AqsiPaymentCodes {
    private val messages =
        mapOf(
            "00" to "Одобрено",
            "000" to "Одобрено",
            "01" to "Позвоните в банк-эмитент",
            "02" to "Позвоните в центр авторизации",
            "03" to "Некорректный merchant ID",
            "04" to "Изъять карту",
            "05" to "Операция отклонена",
            "06" to "Позвоните в центр авторизации",
            "07" to "Изъять карту",
            "08" to "Проверьте документы",
            "09" to "Позвоните в центр авторизации",
            "10" to "Позвоните в центр авторизации",
            "11" to "Позвоните в центр авторизации",
            "12" to "Операция недоступна",
            "13" to "Некорректная сумма",
            "14" to "Неверный номер карты",
            "19" to "Повторная транзакция",
            "21" to "Позвоните в центр авторизации",
            "30" to "Позвоните в центр авторизации",
            "34" to "Изъять карту",
            "41" to "Карта утеряна. Изъять",
            "43" to "Карта украдена. Изъять",
            "51" to "Недостаточно средств",
            "52" to "Операция отклонена",
            "53" to "Операция отклонена",
            "54" to "Просроченная карта",
            "55" to "Неверный PIN-код",
            "56" to "Позвоните в банк",
            "57" to "Транзакция запрещена",
            "58" to "Карта не обслуживается",
            "61" to "Превышен расходный лимит",
            "62" to "Запрещенная карта. Отказ",
            "63" to "Позвоните в центр авторизации",
            "65" to "Операция отклонена",
            "75" to "Исчерпаны попытки ввода PIN",
            "89" to "Неверный MAC-код",
            "91" to "Эмитент недоступен",
            "93" to "Транзакция незаконна",
            "94" to "Повторная транзакция",
            "95" to "Операция не найдена",
            "201" to "Отказ ввода данных",
            "202" to "Транзакция не найдена",
            "203" to "Отказ ввода суммы",
            "212" to "Отказ ввода карты",
            "303" to "Необходимо закрыть смену",
            "304" to "Валюта недоступна",
            "305" to "Карта не обслуживается",
            "401" to "Ошибка чтения карты",
            "402" to "Транзакция не поддерживается",
            "403" to "Ошибка настройки точки доступа",
            "404" to "Ошибка формата ответа хоста",
            "410" to "Ошибка загрузки рабочего ключа",
            "998" to "Операция отклонена терминалом",
            "999" to "Общий системный сбой",
        )

    fun interpret(rawCode: String): AqsiResponseCode {
        val normalized = rawCode.filter { it.isDigit() }.ifEmpty { rawCode.trim().lowercase() }
        val approved = normalized.toIntOrNull() == 0
        val message =
            messages[normalized]
                ?: messages[normalized.trimStart('0').ifEmpty { "0" }]
                ?: if (approved) "Одобрено" else "Операция отклонена терминалом"
        return AqsiResponseCode(
            code = normalized,
            approved = approved,
            message = message,
            timeout = false,
        )
    }

    fun fromReceiptLine(line: String): AqsiResponseCode? {
        val normalized = line.replace(whitespaceRegex, " ").trim()
        if (normalized.isBlank()) return null
        val uppercase = normalized.uppercase()

        responseCodeRegex.find(uppercase)?.let { match ->
            val code = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            if (code != null) return interpret(code)
        }

        return if (uppercase.contains("ОДОБРЕНО")) {
            interpret("00")
        } else {
            null
        }
    }

    private val responseCodeRegex =
        Regex(
            pattern = """(?:КОД\s+ОТВЕТА|RESPONSE\s+CODE)\D*([0-9]{2,3})""",
        )
    private val whitespaceRegex = Regex("\\s+")
}
