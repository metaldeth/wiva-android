# task-03 — отчёт по unit-тестам (Arcus2 / AqsiRepositoryImpl)

**Дата:** 2026-05-12  
**Команда:** `gradlew.bat :app:testDebugUnitTest --tests "com.wiva.android.data.payment.aqsi.*"`

## Результат

| Проверка | Статус |
|----------|--------|
| `:app:testDebugUnitTest` (пакет `com.wiva.android.data.payment.aqsi`) | успех |
| `:app:assembleDebug` | успех |

## Покрытые сценарии (соответствие чеклисту task-03 и замечаниям ревью)

| # | Описание | Класс теста |
|---|----------|-------------|
| 1–3 | Парсер: Approved / Declined / битый STX, обрезка, неверный CRC | `Arcus2ProtocolParsingTest` |
| — | CRC8 контрольное значение KB для строки `123456789` → `0xF4` | `Arcus2ProtocolParsingTest.crc8_matchesKbExample_*` |
| — | BinLen: лишние байты после тела → `failure` (`binlen trailing bytes`) | `Arcus2ProtocolParsingTest.binLen_unwrap_rejectsTrailingBytes` |
| — | Строка `ER` → `Declined` с кодом `er`, не доменный `Error` | `Arcus2ProtocolParsingTest.parser_ok_and_er_paths` |
| — | Внутренний контракт суммы в JPAY START: отрицательные копейки → `IllegalArgumentException` у `buildPaymentOperationStart` (дубль guard на публичном пути) | `Arcus2ProtocolParsingTest.buildPaymentOperationStart_rejectsNegativeKopecks` |
| 4–6 | Недоступный канал / таймаут / IO: маппинг через `Result.failure` у репозитория при отказе клиента | `AqsiRepositoryImplTest` |
| 7–8 | `initiatePayment`: Approved / Declined при успешном `Result` клиента | `AqsiRepositoryImplTest` |
| — | Отрицательная сумма: `Result.failure` с `AqsiTransportException`, клиент не вызывается | `AqsiRepositoryImplTest.initiatePayment_whenAmountNegative_*`, `Arcus2ClientTest.initiatePurchase_negativeAmount_*` |
| — | TCP: ответ `ER` после `BEGINTR:` → `Declined` (сквозной обмен с тестовым сервером) | `Arcus2ClientTest.initiatePurchase_beginTrAckEr_returnsDeclined` |
| — | Отмена / обрыв: `interruptCurrentTcpSession` закрывает сокет, платёж не «висит» до полного `soTimeout` | `Arcus2ClientTest.initiatePurchase_interruptTcp_closesSocketAndFailsFast` |
| 9 | `cancelPayment`: вызов клиента + стартовый `interrupt`; при отмене корутины репозиторий регистрирует второй вызов `interrupt` через `Job.invokeOnCompletion` | `AqsiRepositoryImplTest.cancelPayment_callsClient`; прерывание блокирующего read при реальном сокете — см. тест Arcus2 выше |
| 10 | Holder: обновление после tcp-test / payment | проверки `holder.getSnapshot()?.outcome` в тех же тестах |

### Контракт отмены (репозиторий + блокирующий клиент)

- Перед новой сессией отмены вызывается `interruptCurrentTcpSession()` (закрытие активных сокетов клиента).
- Для `initiatePayment` и `cancelPayment` на активный `Job` вешается `invokeOnCompletion`: при `CancellationException` повторно вызывается `interruptCurrentTcpSession()`, чтобы по возможности разбудить блокирующий `read`/`connect` на открытом сокете.
- Полная кооперативная отмена JVM-потока, заблокированного в `InputStream.read` без `close()` сокета, не гарантируется языком; гарантия «закрыть сокет и тем самым завершить read» покрывается тестом с реальным `Arcus2Client` и `ServerSocket`.

#### Отдельный юнит на `invokeOnCompletion` + отмена в `AqsiRepositoryImpl` (круг 2 ревью)

Отдельный юнит без «спящих» потоков не добавлен: синхронный блокирующий мок клиента на `Dispatchers.IO` в связке с `job.cancel()`/`join()` не даёт стабильного воспроизведения момента вызова обработчика без зависания или искусственных таймаутов; кооперативная отмена корутины при чистом `LockSupport.park()` в том же эксперименте оказалась ненадёжной для CI.

Покрытие цепочки «закрыть TCP → выйти из блокирующего read» зафиксировано интеграционно в `Arcus2ClientTest.initiatePurchase_interruptTcp_closesSocketAndFailsFast`; wiring репозитория (`invokeOnCompletion` + вызов `interruptCurrentTcpSession`) остаётся статически проверяемым по коду и описан здесь.

### PCB в TCP-фикстурах сервера (`Arcus2ClientTest`)

Ответы тестового сервера кодируются с `pcb = 0`; у исходящих кадров клиента PCB чередуется по правилам Arcus2. Декодер входящих кадров проверяет STX/длину/CRC и не требует строгого чередования PCB «как на железе». Для юнит-среды этого достаточно; расхождение PCB на реальной линии диагностируется по KB и при необходимости отдельными приёмочными тестами.

Реальный TCP-сокет в большинстве юнит-тестов не поднимается; транспорт репозитория замещён `RecordingArcusClient`.
