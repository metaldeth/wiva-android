# task-03 — Arcus2 TCP-клиент и AqsiRepositoryImpl

**Зависимости:** task-02

## Связь с юзер-кейсом

UC-2: оплата через aQsi; UC-4: основа для теста TCP; UC-5: **in-memory** сводка последней операции (обновление из репозитория для сеансов TCP-теста, платежа и отмены — см. § ниже; отображение на вкладке — **task-06**; поток заказа — **task-05**).

## Что сделать

- `Arcus2Client`: TCP connect/read/write, сборка/разбор кадров Arcus2 по спецификации из `docs/payment-aqsi-pill-t7100/`; операции уровня JPAY для сценария оплаты и отмены.
- **Байтовый лог** — только в debug-сборках, тег **`Arcus2`**; не логировать PAN/трек/CVV.
- `AqsiRepositoryImpl`: `Dispatchers.IO` (или `withContext(IO)`), маппинг ошибок в `Result` / `AqsiPaymentResult.Error`; вызовы клиента; персистентность `AqsiConfig` под ключом `AQSI_SETTINGS`.
- Реализация **`testTcpConnection`**: TCP connect + graceful close в пределах `timeoutMs`; при необходимости минимальный handshake **без** инициации платёжной транзакции (см. architecture.md).
- Логи репозитория — тег **`AqsiRepo`** (агрегировано, не сырые байты как в Arcus2-debug).

### Альтернативы UC-2 (чеклист уровня data)

- **Decline:** ответ протокола → `AqsiPaymentResult.Declined` с безопасным кодом/причиной для UI (без PAN/трека).
- **Таймаут:** превышение `timeoutMs` на connect/read → контролируемая ошибка (без uncaught exception на корутине).
- **Обрыв сокета:** `IOException` / connection reset / broken pipe → `Error` + лог; не бросать наружу в необработанном виде.
- **Invalid packet:** битый STX/LEN/CRC/обрезанный буфер → ошибка парсинга, **не** успех и **не** crash.
- **Отмена:** `cancelPayment()` корректно завершает попытку с точки зрения сокета/сессии; результат для верхнего уровня — согласовать с **task-05** (отмена без пост-успеха).

### In-memory диагностика (UC-5, слой репозитория)

- Ввести **тип данных** сводки и **holder** (класс в `data`/`domain`, имена по конвенции): только нечувствительные поля по `architecture.md` (время, исход approve/decline/error/cancel/тест TCP, краткий код, без сырых payload). **`@Singleton` + `@Provides` / конструктор для Hilt** — в **task-04**; в **task-03** репозиторий принимает holder через конструктор (временно можно `Default` в тестах до появления модуля).
- После **`testTcpConnection`** (успех/ошибка), после **`initiatePayment`** (любой исход), после **`cancelPayment`** (если применимо к диагностике) — **обновлять** holder так, чтобы вкладка диагностики и сценарий «оплата с экрана заказа» (через тот же `initiatePayment`) видели актуальную сводку без перезапуска процесса.

## Точки изменения

- `data/payment/aqsi/Arcus2Client.kt` (новый)
- `data/payment/aqsi/AqsiRepositoryImpl.kt` (новый)
- при необходимости вспомогательные классы парсера в том же пакете (новые)
- при необходимости `…/AqsiLastOperationSummary.kt` + `…/AqsiLastOperationSnapshotHolder.kt` (или эквивалент) — **классы в этом таске**; **провайдер Hilt** — **task-04**

**Не трогать:** `PaymentTerminalService.kt`, контроллер, `IntegrationsModule.kt`.

## Тест-кейсы (TDD / unit)

1. **Парсер ответного пакета — Approved:** fixture `ByteArray` → распознан успех (approved).
2. **Парсер — Declined:** fixture → decline с ожидаемым безопасным кодом/причиной.
3. **Парсер — некорректный кадр (invalid packet):** короткий мусор / неверная CRC / обрезанные данные → ошибка протокола без падения тестового раннера.
4. **`testTcpConnection` — недоступный хост:** мок сокета или тестовый сервер: ожидаемый `Result.failure` / ошибочная ветка без необработанного исключения на корутине (по принятому в проекте стилю).
5. **Таймаут read/connect:** замоканный транспорт, имитирующий отсутствие ответа до `timeoutMs` → ошибка таймаута, без uncaught.
6. **Обрыв сокета:** замоканный клиент бросает `IOException` (например broken pipe) → маппинг в `Error` / `Result.failure` по контракту.
7. **`initiatePayment` маппинг — Approved:** при замоканном клиенте, возвращающем успешный распарсенный результат → `Result.success(Approved)`.
8. **`initiatePayment` — Declined:** замоканный клиент отдаёт decline-фикстуру → `Declined`, не `Success`.
9. **`cancelPayment`:** при замоканном клиенте проверить вызов отмены / согласованный итог (без успешного approve после отмены).
10. **Диагностика holder (unit):** после `testTcpConnection` или `initiatePayment` (fake repo + fake client) in-memory сводка содержит ожидаемый статус и тип операции (если holder вводится в этом таске; иначе перенести в тест оркестратора **task-05**).

Использовать фикстуры из спецификации KB; не включать реальные PAN/CVV в репозиторий тестов.

## Критерий этапа (ТЗ)

A3 (unit-тест парсера обязателен).
