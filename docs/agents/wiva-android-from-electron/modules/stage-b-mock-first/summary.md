# Итог: stage-b-mock-first, B3 (MockController / шлюз)

## Сделано

- Пакет `com.wiva.android.hardware.controller`: `RequestCommand` / `ResponseCommand` (коды 1:1 с wiva_electron), `ControllerResponseEvent`, `ControllerGateway`.
- `MockControllerGateway` — журнал исходящих команд, `MutableSharedFlow` (replay 0, extraBufferCapacity 64), `simulateResponse`, логи с тегом `WivaController`.
- `StubRealControllerGateway` — только лог «real stub», без эмиссий во входящий поток.
- `DelegatingControllerGateway` — на каждый вызов читает `ConfigRepository.get(JsonStoreKeys.USE_MOCK_CONTROLLER) == "true"`, делегирует моку или стабу; `incomingResponses` = `merge(mock, stub).shareIn(@AppIoScope, Eagerly)`; `simulateResponseForTests` вызывает мок только в мок-режиме.
- Hilt: `ControllerModule` — `@Binds ControllerGateway` → `DelegatingControllerGateway`.
- Сервис: кнопка «Тест контроллера (B3)» → `ServiceViewModel.runControllerSelfTest()` (ReadFirmwareVersion + симуляция `ControllerVersionAnswer`).
- Юнит-тест: `MockControllerGatewayTest` (запись команды, эмиссия симуляции).

## Как воспроизвести приёмку п.5 (эмулятор)

1. Собрать и установить debug/release APK.
2. Открыть **Сервис**, включить **Мок контроллера**.
3. Нажать **Тест контроллера (B3)**.
4. В logcat по тегу **WivaController**: строки `mock TX: ReadFirmwareVersion`, `mock RX simulate: ControllerVersionAnswer`, затем `UC-3 self-test finished`.
5. С моком выключенным: та же кнопка шлёт в стаб (`real stub TX`); симуляция ответа не выполняется (no-op).

## Отличия от имён в wiva Electron

- Kotlin-идиомы: `SentCommandRecord` вместо сырого списка кортежей; `simulateResponse` / `simulateResponseForTests` — разделение мок-реализации и публичного шлюза.
