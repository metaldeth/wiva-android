# task-02 — Домен aQsi: конфиг, результаты, интерфейс репозитория

**Зависимости:** task-01

## Связь с юзер-кейсом

UC-4, UC-5 (контракт данных); подготовка к реализации TCP/JPAY без клиента в этом таске.

## Что сделать

- `AqsiConfig`: `host`, `port` (default 16107), `timeoutMs: Long` (default 15000, как в `architecture.md`).
- `AqsiPaymentResult`: sealed Approved / Declined / Error (без чувствительных данных в сообщениях).
- Интерфейс `AqsiRepository` по `architecture.md`:
  - `loadConfig()` / `saveConfig()`
  - `testTcpConnection()` — проверка канала без финансовой транзакции
  - `initiatePayment(amountKopecks)` / `cancelPayment()`
- Добавить в `JsonStoreKeys` **только** `AQSI_SETTINGS` (`"aqsiSettings"`).

## Точки изменения

- `domain/model/AqsiConfig.kt` (новый)
- `domain/model/AqsiPaymentResult.kt` (новый)
- `domain/repository/AqsiRepository.kt` (новый)
- `JsonStoreKeys.kt` — второй новый ключ (после task-01 для минимизации конфликтов в одном файле)

Реализации клиента и DI — **не** в этом таске.

**Не трогать:** запрещённые файлы из ТЗ.

## Тест-кейсы (TDD / unit)

1. **`AqsiConfig` дефолты:** при конструировании из дефолтов / пустого JSON (если есть утилита сериализации в этом таске — иначе перенести в task-03) — ожидаемые port и timeout.
2. **Равенство/копирование конфига** (если используется в тестах downstream): стабильность data class.
3. **Варианты `AqsiPaymentResult`:** покрыть все sealed-ветки для будущего маппинга (можно через простые assert на типы / equality где применимо).

*Примечание:* основная логика `AqsiRepository` тестируется в task-03 через fake/mocks; здесь — модели и контракт без сети.

## Контрольная сборка

После завершения: **`gradlew.bat assembleDebug`** (этап A2).

## Критерий этапа (ТЗ)

A2.
