# task-05 — CardPaymentOrchestrator и оба карточных входа DrinkListViewModel

**Зависимости:** task-01, task-04

## Связь с юзер-кейсом

UC-1 (PAX), UC-2 (aQsi), **UC-1b / G3** (СБП не трогать); архитектурное ревью: **два** входа карты на экране заказа.

## Что сделать

- Ввести унифицированный `CardPaymentResult` (Success / Failed / Cancelled), если ещё не добавлен отдельным файлом в task-04-подготовке — разместить в `domain/model/`.
- **`CardPaymentOrchestrator`**:
  - Зависимости: `PaymentTerminalService` (без изменений класса), `AqsiRepository`, `CardPaymentMethodRepository`.
  - `suspend fun pay(...)` — **единый** контракт параметров (или параметр-носитель), покрывающий **оба** текущих вызова `sendSumToTerminal` без дублирования бизнес-правил 0x48: напиток (`paidTerminalThenPour`, `sbp == false`) и подписка (`startSubscriptionPayment`, `isSbp == false`). Аргументы должны быть **эквивалентны** тем, что сегодня передаются в терминал в каждом месте.
  - Ветка **PAX:** только делегирование в `sendSumToTerminal(...)` с теми же аргументами, что раньше в соответствующем месте.
  - Ветка **AQSI:** `initiatePayment(amountKopecks)` + маппинг в `CardPaymentResult`.
  - Отмена: PAX — существующий механизм отмены терминала как сейчас в VM; AQSI — `cancelPayment()` (согласовать с жизненным циклом платежа в VM без изменения `PaymentTerminalService`).
  - Логи оркестратора — тег **`CardPayment`**.
- **`DrinkListViewModel`** (`ui/screens/customer/DrinkListViewModel.kt`):
  1. **`paidTerminalThenPour`:** заменить прямой `paymentTerminalService.sendSumToTerminal` на **`cardPaymentOrchestrator.pay(...)`** **только** в ветке **`sbp == false`**. Ветка **`sbp == true`** — **без изменений** логики и без вызова оркестратора карты.
  2. **`startSubscriptionPayment`:** в ветке **`isSbp == false`** заменить прямой `sendSumToTerminal` на **тот же** `cardPaymentOrchestrator.pay(...)` с параметрами, эквивалентными текущему вызову подписки. Ветка **`isSbp == true`** — не трогать.
- После **успеха** любой карточной ветки сохранить существующий код налива / `saleSubscribeTopic` / телеметрии (**method = CARD** в JSON для успешной карты).

### Альтернативы UC-1 / UC-2 (чеклист приёмки)

- **UC-1 PAX:** decline / таймаут / отмена — поведение **как до** оркестратора (регрессия): без лишнего налива и без **успешной** sale-телеметрии при неуспехе; сообщения и возврат в безопасное состояние заказа — как в текущем приложении.
- **UC-2 AQSI:**
  - **Declined** (`AqsiPaymentResult.Declined`) → `CardPaymentResult` неуспех; **нет** налива, **нет** успешной телеметрии.
  - **Timeout / socket / invalid packet / прочая ошибка** → неуспех; **нет** пост-успеха.
  - **Отмена** → согласованный `Cancelled` / неуспех; **нет** налива и **нет** успешной телеметрии.
- **Диагностика (UC-5, поток заказа):** при **любом** завершении AQSI-платежа, инициированного с **экрана заказа** через оркестратор (approve / decline / error / cancel), **in-memory** сводка последней операции (**holder** из **task-03**/**task-04**) должна обновляться так же, как при вызовах репозитория из других мест. Если репозиторий уже пишет сводку внутри `initiatePayment`/`cancelPayment`, оркестратор **не дублирует** запись; иначе — явное обновление из оркестратора после результата ветки AQSI. Трассировка: см. **task-06** (отображение).

## Точки изменения

- `services/payment/CardPaymentOrchestrator.kt` (новый)
- `domain/model/CardPaymentResult.kt` (новый, если выносится из оркестратора)
- `ui/screens/customer/DrinkListViewModel.kt` — инъекция оркестратора и две точки замены вызова

**Не трогать:** `PaymentTerminalService.kt`, `hardware/controller/**`, `PaymentMethod.kt`, `IntegrationsModule.kt`, логику СБП/freeMode/mock controller кроме точек инъекции и замены **чистой** карты.

## Тест-кейсы (TDD / unit + интеграция ViewModel по возможности)

### Оркестратор (unit, моки зависимостей)

1. **PAX выбран:** `pay` вызывает ровно один раз `sendSumToTerminal` с аргументами, переданными из теста; `AqsiRepository.initiatePayment` **не** вызывается.
2. **AQSI выбран:** `pay` вызывает `initiatePayment(expectedKopecks)`; `sendSumToTerminal` **не** вызывается.
3. **AQSI → Approved:** результат `CardPaymentResult.Success`.
4. **AQSI → Declined/Error/Timeout (маппинг из repo):** `Failed` или согласованный неуспех **без** исключения наружу; **verify**, что к мокам телеметрии успеха / налива нет обращения (где применимо — через зависимости пост-успеха).
5. **AQSI → Cancelled:** результат отмены **без** успешной телеметрии и без запуска налива.
6. **Отмена при активном AQSI:** вызывается `cancelPayment()` (или согласованный контракт с VM).
7. **Отмена при PAX:** делегирование в тот же тип отмены, что использует текущий код (зафиксировать через spy/mock терминального сервиса без изменения его класса).
8. **PAX → неуспех (regression):** при замоканном `sendSumToTerminal`, возвращающем неуспех, оркестратор не превращает это в успех и не вызывает AQSI-ветку.

### DrinkListViewModel (unit / kotlinx-coroutines test)

9. **Напиток, карта (`sbp == false`):** при выборе карточного потока используется оркестратор (verify), не прямой `sendSumToTerminal` на сервисе.
10. **Напиток, ветка `sbp == true`:** оркестратор карты **не** вызывается; последовательность как до изменений (regression).
11. **Подписка `isSbp == false`:** используется оркестратор с теми же параметрами, что раньше уходили в `sendSumToTerminal` для подписки (verify).
12. **Подписка `isSbp == true`:** без вызова оркестратора карты; поведение как раньше.
13. **AQSI неуспех с заказа — без пост-успеха:** при моке `initiatePayment` на decline/error VM не входит в ветку успешного налива / не дергает успешную sale-телеметрию (через verify зависимостей, если доступно в тестах VM).
14. **Диагностика после AQSI с заказа:** после завершения `pay` в ветке AQSI (любой исход) in-memory holder обновлён (read-after через тот же fake / `test scheduler`, если holder инжектируется в тест).

*Мок контроллера / PAX:* ручная или автоматическая верификация PAX-ветки на моке по ТЗ A5 — вне unit-тестов, если инфраструктуры нет; тогда зафиксировать в MR чеклисте офисной проверки.

## Контрольная сборка

После завершения: **`gradlew.bat assembleDebug`** (этап A5).

## Критерий этапа (ТЗ)

A5.
