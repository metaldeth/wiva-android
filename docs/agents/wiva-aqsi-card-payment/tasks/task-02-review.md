# Code review — task-02: домен aQsi (круг 2)

**SessionId:** `wiva-aqsi-card-payment`  
**Задача:** `task-02 — Домен aQsi: конфиг, результаты, интерфейс репозитория`  
**Контекст:** повторное ревью после исправления `timeoutMs: Long` по замечанию круга 1. Код агентом не изменялся; оценка по актуальным файлам и докам.

## Итог

Критических замечаний нет. Доменный контракт по `timeoutMs` согласован с `task-02.md`, `request.md` (A2) и `architecture.md`: тип `Long`, дефолт 15000 мс через `AQSI_DEFAULT_TIMEOUT_MS = 15_000L`. Новых содержательных проблем по объёму task-02 не выявлено.

## Закрытие замечания круга 1

- **Было (Medium):** `timeoutMs` как `Int` при документации `Long`.
- **Сейчас:** в `AqsiConfig` поле `timeoutMs: Long = AQSI_DEFAULT_TIMEOUT_MS`, константа `15000L`; тесты используют литералы `Long` там, где нужно (`3000L`, `5000L`); сравнение с `AQSI_DEFAULT_TIMEOUT_MS` в дефолтных кейсах корректно.

## Проверенные файлы

- `app/src/main/java/com/wiva/android/data/local/db/JsonStoreKeys.kt`
- `app/src/main/java/com/wiva/android/domain/model/AqsiConfig.kt`
- `app/src/main/java/com/wiva/android/domain/model/AqsiPaymentResult.kt`
- `app/src/main/java/com/wiva/android/domain/repository/AqsiRepository.kt`
- `app/src/test/java/com/wiva/android/domain/model/AqsiConfigTest.kt`
- `app/src/test/java/com/wiva/android/domain/model/AqsiPaymentResultTest.kt`

## Замечания

Существенных замечаний нет.

## Проверка требований task-02

- `AqsiConfig`: `host`, `port` (дефолт 16107), `timeoutMs: Long` (дефолт 15000); покрыто тестами дефолтов, `{}`, частичного JSON, copy/equals, round-trip JSON.
- `AqsiPaymentResult`: sealed `Approved` / `Declined` / `Error`; без чувствительных полей; тесты по ветк и exhaustiveness `when`.
- `AqsiRepository`: `loadConfig`, `saveConfig`, `testTcpConnection`, `initiatePayment(amountKopecks)`, `cancelPayment`; KDoc `testTcpConnection` ссылается на `AqsiConfig.timeoutMs` — согласовано с типом `Long`.
- `JsonStoreKeys`: для задачи добавлен только `AQSI_SETTINGS = "aqsiSettings"`; остальные ключи не тронуты.
- Документация пайплайна: `task-02.md`, `task-02-test-report.md`, `architecture.md`, `request.md` — формулировки про `timeoutMs` и A2 согласованы с кодом.
- `task-02-test-report.md`: зафиксированы успешные `assembleDebug` и целевые unit-тесты после правки типа.

## Риски / напоминания для task-03 (вне критичности A2)

- При вызове JDK/Android API с таймаутом в `Int` (мс) реализация может приводить `Long` к `Int` с явными границами — это ожидаемая ответственность слоя impl, а не регресс доменной модели.
- Санитаризация сообщений в `AqsiPaymentResult.Error` по-прежнему должна обеспечиваться в data-слое, чтобы в UI не попадали сырые исключения или PCI-чувствимые строки.
