# task-05 — отчёт по тестам (CardPaymentOrchestrator / DrinkListViewModel)

## Команды (фактический прогон 2026-05-12)

```text
# §14 Approved: узкий прогон интеггации VM после `task05_14_aqsiApprovedFromOrder_updatesDiagnosticHolder`
gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.viwa.android.ui.screens.customer.DrinkListViewModelTask05IntegrationTest"

gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.viwa.android.services.payment.CardPaymentOrchestratorTest" --tests "com.viwa.android.ui.screens.customer.DrinkListViewModelPaymentFlowTest" --tests "com.viwa.android.ui.screens.customer.DrinkListViewModelTask05IntegrationTest"

gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.viwa.android.data.payment.aqsi.AqsiRepositoryImplTest" --tests "com.viwa.android.domain.model.AqsiPaymentResultTest"

gradlew.bat --no-daemon assembleDebug
```

## Результаты

| Команда | Результат |
|---------|-----------|
| `DrinkListViewModelTask05IntegrationTest` (включая §14 Approved) | BUILD SUCCESSFUL |
| Оркестратор + VM (§9–14) | BUILD SUCCESSFUL |
| `AqsiRepositoryImplTest` + `AqsiPaymentResultTest` | BUILD SUCCESSFUL |
| `assembleDebug` | BUILD SUCCESSFUL |

## Покрытие сценариев task-05 (оркестратор)

| Пункт задачи | Файл теста |
|--------------|------------|
| PAX: `pay` → один вызов `sendSumToTerminal`, без `initiatePayment` | `CardPaymentOrchestratorTest.pay_whenPaxSelected_*` |
| AQSI: `initiatePayment(expectedKopecks)`, без терминала | `CardPaymentOrchestratorTest.pay_whenAqsiSelected_*` |
| AQSI Approved / Declined / Error / Cancelled / failure `Result` | `CardPaymentOrchestratorTest.pay_aqsi*` |
| PAX ошибка исключением → Failed | `CardPaymentOrchestratorTest.pay_paxThrows_*` |
| Отмена: AQSI во время платежа → `cancelPayment` | `CardPaymentOrchestratorTest.cancelActivePayment_duringAqsiPay_*` |
| Отмена: PAX во время платежа → `cancelTransaction` | `CardPaymentOrchestratorTest.cancelActivePayment_duringPaxPay_*` |

## DrinkListViewModel и `DrinkListCardPaymentFlow`

Логика карты/СБП для напитка и подписки вынесена в **`DrinkListCardPaymentFlow`** (`ui/screens/customer/DrinkListCardPaymentFlow.kt`) для прямых unit-тестов без тяжёлого VM.

Тесты с полным **`DrinkListViewModel`** используют **`internal fun setUiStateForUnitTests`**: начальное состояние задаётся без ручного прохождения сценариев сканов/телеметрии.

| Пункт task-05.md | Тест | Где |
|------------------|------|-----|
| §9 Напиток, карта: оркестратор, не прямой `sendSumToTerminal` | `task05_9_drinkCardUsesOrchestratorNotDirectTerminal` | `DrinkListViewModelPaymentFlowTest` |
| §10 Напиток СБП: без оркестратора, `sendSumToTerminal` | `task05_10_drinkSbpDoesNotCallCardOrchestratorPay` | `DrinkListViewModelPaymentFlowTest` |
| §11 Подписка карта: оркестратор с параметрами как раньше у терминала | `task05_11_subscriptionCardCallsOrchestratorLikeLegacyTerminal` | `DrinkListViewModelPaymentFlowTest` |
| §12 Подписка СБП: без оркестратора | `task05_12_subscriptionSbpDoesNotCallCardOrchestrator` | `DrinkListViewModelTask05IntegrationTest` |
| §13 Неуспех с заказа: без пост-успеха (подписка / напиток) | `task05_13_subscriptionCardDecline_doesNotSendSaleSubscribeTopic`, `task05_13_drinkCardDecline_doesNotCallPrepareDrink` | `DrinkListViewModelTask05IntegrationTest` |
| §14 Диагностика после AQSI с заказа (approved / decline / error / cancel) | `task05_14_aqsiApprovedFromOrder_updatesDiagnosticHolder`, `task05_14_aqsiDeclineFromOrder_updatesDiagnosticHolder`, `task05_14_aqsiErrorFromOrder_updatesDiagnosticHolder`, `task05_14_aqsiCancelledFromOrder_updatesDiagnosticHolder` | `DrinkListViewModelTask05IntegrationTest` |

### Техника для §12–14

- **`Dispatchers.setMain`** на однопоточный `ExecutorService`: `viewModelScope` и реальный **`PaymentTerminalService`** (подписка на `incomingResponses`) не блокируют JUnit.
- Тело сценария — **`runBlocking { }`** на потоке JUnit (не на `Main`), иначе deadlock с тем же диспетчером, что и `Main`.
- Синхронизация вместо **`delay(…)`**: **`awaitCondition`** (цикл с **`withContext(Main)`** + **`yield`**) до явного **`DrinkListUiState`** / **`AqsiLastOperationSnapshotHolder.getSnapshot()`**, затем короткий **`flushMain`** перед verify.
- Репозиторий **AQSI** в §14 — **`AqsiRepositoryImpl`** с тестовым **`Arcus2TerminalClient`** и **`Dispatchers.Unconfined`** для IO, плюс **`AqsiLastOperationSnapshotHolder`** (`app/src/main/java/com/wiva/android/data/payment/aqsi/AqsiLastOperationSummary.kt`).

### `DrinkListViewModel.onCleared` и отмена платежа

- В production **`onCleared` не вызывает `runBlocking`**: отмена активного платежа идёт через **`paymentClearingScope.launch { runCatching { cardPaymentOrchestrator.cancelActivePayment() } }`** на **`Dispatchers.IO`** (`companion` scope в `DrinkListViewModel`).
- Вызов **fire-and-forget**: поток, на котором дергается **`onCleared`**, не ждёт завершения отмены; отмена **best-effort** и асинхронна относительно **`paymentJob?.cancel()`** и **`super.onCleared()`**. Отдельный синхронный контракт здесь не интроспектируется в unit-тестах ( **`onCleared`** — `protected` API `ViewModel`), поведение зафиксировано в этом отчёте и в коде.

## Диагностика aQsi (in-memory holder)

Обновление сводки при завершении AQSI-платежа с экрана заказа выполняется внутри **`AqsiRepositoryImpl`** (`recordPayment` / агрегат), без дублирования в **`CardPaymentOrchestrator`**.
