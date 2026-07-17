package com.wiva.android.services.payment

/**
 * Классификация записи журнала платёжника для UX (аналог TX/RX в дебаге контроллера).
 *
 * [ToTerminal] — команда/запрос к железу или шлюзу терминала.
 * [FromTerminal] — статус или результат от терминала/ридера.
 * [Mock] — сценарий без физического платёжника.
 * [System] — настройки сервисного меню, отмена локально, диагностические подстановки.
 */
enum class CardPaymentLogLane {
    ToTerminal,
    FromTerminal,
    Mock,
    System,
}
