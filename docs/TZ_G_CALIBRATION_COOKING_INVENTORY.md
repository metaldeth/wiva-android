# ТЗ: Модуль G — Калибровки, готовка напитка, остатки

> Эталон поведения: **wiva_electron**. Ссылки на конкретные файлы внутри. Детали реализации на стороне агента — из кода электрона.

## Контекст

Физический контроллер отлажен (этапы B–F завершены). Теперь реализуем полный цикл:
**калибровка воды → калибровка сиропов → готовка напитка → списание остатков → телеметрия**.

Разбивка на независимые задачи: каждую запускать отдельным **complex**.

---

## G1 — Калибровка воды

**Вкладка сервисного меню:** «Калибровка» → подвкладка «Вода» (по структуре из `viwa_electron/src/renderer/components/ServiceMenu/constants.ts`).

### Эталон (electron)

| Что | Файл electron |
|---|---|
| UI вкладки | `src/renderer/components/ServiceMenu/tabs/WaterCalibrationTab/WaterCalibrationTab.tsx` |
| UI модалки налива | `src/renderer/components/ServiceMenu/tabs/CalibrationTab/WaterCalibrationModal/WaterCalibrationModal.tsx` |
| IPC-каналы | `src/common/ipc/controller.ts` — `WATER_CALIBRATION_POUR`, `WATER_CALIBRATION_WRITE_COEFFICIENT`, `GET_WATER_CALIBRATION_INFO`, событие `WATER_CALIBRATION_POUR_RESULT` |
| Реализация на main | `src/main/modules/controller/index.ts` — хендлеры от строки 416 |

### Алгоритм

1. **Тестовый налив** (`WATER_CALIBRATION_POUR`): посылает `ServiceCommand` **0x52**, режим **0x0A**, тело `[0x0a, 0, 0, 0, volumeMl/10]`.
   - Слушаем `DrinkPreparingBegin` (фиксируем `t0 = now()`).
   - Слушаем `DrinkPreparingSuccess` (фиксируем `endMs`, вычисляем `durationSec = (endMs - t0) / 1000`).
   - Таймаут ожидания SUCCESS: 120 с.
   - Сохраняем `lastPourDurationSec` + временны́е метки в ключ `'waterCalibration'` локального хранилища (аналог `configService.updateConfig`).
   - Уведомляем UI: `durationSec` (или ошибку если таймаут / `durationSec < 1 с`).

2. **Ввод фактического объёма пользователем** — UI показывает поле для ввода `actualVolumeMl`.

3. **Запись коэффициента** (`WATER_CALIBRATION_WRITE_COEFFICIENT`): payload — `targetVolumeMl`, `actualVolumeMl`, `lastPourDurationSec`.
   - Читаем текущий коэффициент помпы: `ReadWaterPumpModel` **0xBC** → ответ `WaterPumpModelAnswer`, байт `currentTenths`.
   - Вычисляем: `newTenths = round(currentTenths * targetVolumeMl / actualVolumeMl)`, clamp 1–255.
   - Пишем: `WriteWaterPumpModel` **0xBB**, тело `[newTenths x5]`, ждём `ControllerACK`.
   - **Сохраняем** в `'waterCalibration'`:
     ```
     flowRateMlPerSec = actualVolumeMl / lastPourDurationSec
     lastTargetMl     = targetVolumeMl
     lastActualMl     = actualVolumeMl
     coefficient      = actualVolumeMl / targetVolumeMl
     lastPourDurationSec
     ```

4. **Чтение сохранённых данных** (`GET_WATER_CALIBRATION_INFO`): возвращает объект `WaterCalibrationInfo` из хранилища.

### Важно о `flowRateMlPerSec`

- **Обязательное поле для готовки.** Если в хранилище нет `flowRateMlPerSec > 0` — готовка недоступна (показываем предупреждение в UI, не используем дефолт).
- В electron есть дефолт 20 мл/с (`DEFAULT_FLOW_RATE_ML_PER_SEC`) — **не копировать**. Именно он приводил к неверному времени готовки на физическом оборудовании.

### DoD G1

- [ ] Тестовый налив отправляет команду на контроллер; в моке — имитирует BEGIN+SUCCESS через таймеры
- [ ] `durationSec` и временны́е метки сохраняются в хранилище
- [ ] После ввода фактического объёма: коэффициент записан на контроллер (`WriteWaterPumpModel` + ACK) и `flowRateMlPerSec` сохранён в хранилище
- [ ] Вкладка сервисного меню показывает последние данные калибровки
- [ ] Unit-тест: пересчёт `newTenths`, расчёт `flowRateMlPerSec`

---

## G2 — Калибровка сиропов

**Вкладка сервисного меню:** «Калибровка» → подвкладки по контейнерам.

### Эталон (electron)

| Что | Файл electron |
|---|---|
| UI | `src/renderer/components/ServiceMenu/tabs/CalibrationTab/CalibrationTab.tsx` |
| IPC | `src/common/ipc/controller.ts` — `CALIBRATE_CONTAINER`; `src/common/ipc/inventory.ts` — `SUBMIT_CALIBRATION_RESULT` |
| Тестовый налив (main) | `src/main/modules/controller/index.ts` — хендлер `CALIBRATE_CONTAINER` от строки 340 |
| Запись результата (main) | `src/main/modules/inventory/index.ts` — функция `submitCalibrationResult` |

### Алгоритм

1. **Тестовый налив** (`CALIBRATE_CONTAINER`): payload — `containerNumber`, `targetProductMl`.
   - physicalPort = `containerNumber + 8` (маппинг как в electron).
   - `dispenserWorkTimeSec = targetProductMl / dosage.conversionFactor`.
   - Посылает `ServiceCommand` **0x52**, режим **0x09**, тело `[0x09, 0, physicalPort, dispenserTimeTenths, 0]`.

2. **Ввод фактического объёма** — пользователь вводит `actualVolumeMl`.

3. **Запись результата** (`SUBMIT_CALIBRATION_RESULT`): payload — `containerNumber`, `actualVolumeMl`, `targetProductMl`.
   - `newCF = currentCF * (actualVolumeMl / targetProduct)` — обновляет `dosage.conversionFactor`.
   - Сохраняет в конфиг контейнеров.
   - Отправляет в телеметрию: `cellVolumeImportTopic` **и** `cellStoreImportTopic`.
   - Если WebSocket отключён — предупреждение в лог, не ошибка.

### DoD G2

- [ ] Тестовый налив отправляет команду контроллеру; в моке — эмулирует ответ
- [ ] `conversionFactor` пересчитывается по формуле и сохраняется
- [ ] После сохранения — оба топика телеметрии отправлены (или залогировано предупреждение)
- [ ] Unit-тест: расчёт `newCF`, корректность тела команды `ServiceCommand`

---

## G3 — Процесс готовки напитка

### Эталон (electron)

| Что | Файл electron |
|---|---|
| Orchestration (prepareDrink) | `src/main/modules/payment/preparing/manager.ts` |
| ChooseDrink (рецепт) | `src/main/services/drink/DrinkSelectionService.ts` |
| StartDrinkPreparing | `src/main/services/drink/DrinkPreparingService.ts` |
| Тело команды ChooseDrink | `src/main/hardware/controller/commands/chooseDrinkBody.ts` |
| Состояния (enum) | `src/common/types/PreparingDTO.ts` — `PreparingState` |
| UI экрана готовки | `src/renderer/pages/Preparing/` |
| Android (уже есть) | `services/drink/ViwaDrinkPreparingService.kt`, `ui/screens/customer/PreparingScreen.kt` |

### Последовательность

```
prepareDrink(tasteId, volume, waterOption?, concentrationRatio?)
  1. ensureAutoMode() — перевести контроллер в авто-режим (таймаут 5 с)
     └── fail → FAIL с errorCode AUTO_MODE_SWITCH_FAILED
  2. Найти контейнер по tasteId в конфиге
     └── не найден → FAIL с errorCode CONTAINER_NOT_FOUND
  3. Читаем flowRateMlPerSec из 'waterCalibration'
     └── отсутствует или ≤ 0 → FAIL с errorCode WATER_NOT_CALIBRATED
  4. chooseDrink → посылает ChooseDrink 0x50, возвращает preparingTime
  5. Ждём 200 мс
  6. startDrinkPreparing(preparingTime) → посылает StartDrinkPreparing 0x55
```

### Расчёт `preparingTime`

```
ratio                 = volume / dosage.drinkVolume
productAmount         = dosage.product * ratio * concentrationRatio
dispenserWorkTimeSec  = productAmount / dosage.conversionFactor
waterMl               = dosage.water * ratio
preparingTime         = round(waterMl / flowRateMlPerSec)   ← время для UI-прогресса
```

- `flowRateMlPerSec` — **только из калибровки воды** G1, дефолт запрещён.
- `preparingTime` — время для прогресс-бара UI; реальное окончание определяет контроллер (`DrinkPreparingSuccess`).

### Тело команды ChooseDrink (0x50): 9 байт

```
[0x01, port, timeByte, waterByte, 0, 0, 0, 0, tof]
port      = clamp(1, 255, containerNumber + 8)
timeByte  = clamp(0, 255, round(dispenserWorkTimeSec * 10))   // 0.1 с/бит
waterByte = clamp(0, 100, round(waterMl / 10))                // 10 мл/бит
tof       = waterOption из маппинга или sodaStatus == true → 2, иначе 0
```

Подробности: `chooseDrinkBody.ts` (строки 23–40).

### StartDrinkPreparing (0x55)

Тело: `[0, 0, 1, 1, 0]` (флаги S-Model — не менять).

### Состояния

`PreparingState`: `START_PREPARING` → `BEGIN` → `SUCCESS` | `FAIL` | `CUP_TAKEN`

На `BEGIN` — стартуем прогресс-бар с `preparingTime` секундами.  
На `SUCCESS` — переходим к экрану "напиток готов", вызываем G4.

### Мок

Если `isMockPort`:
- через 1 с эмитировать `BEGIN`
- через `1 + preparingTime` с эмитировать `SUCCESS`

### DoD G3

- [ ] Happy-path на моке: выбор напитка → `BEGIN` → `SUCCESS`, прогресс-бар отображается
- [ ] Без данных калибровки воды → `FAIL` + сообщение пользователю
- [ ] Контроллер не в авто-режиме → попытка переключения, при неудаче — `FAIL`
- [ ] Контейнер не найден → `FAIL`
- [ ] Unit-тест: `buildChooseDrinkBody`, расчёт `preparingTime`
- [ ] На физическом контроллере: команды доходят, контроллер отвечает `BEGIN`/`SUCCESS`

---

## G4 — Остатки и телеметрия

### Эталон (electron)

| Что | Файл electron |
|---|---|
| Списание после SUCCESS | `src/main/modules/payment/preparing/manager.ts` — метод `applyInventoryWriteOff` (строки 81–112) |
| Ручное редактирование остатков | `src/main/modules/inventory/index.ts` — `applyCellVolumes` |
| UI остатков | `src/renderer/components/ServiceMenu/tabs/InventoryTab/InventoryTab.tsx` |
| cellVolumeImportTopic | `src/main/modules/telemetry/manager.ts` — `sendCellVolumeImportFromConfig` |
| saleImportTopic | `src/main/modules/telemetry/manager.ts` — `sendSaleImportTopic` |
| Структура `SaleImportBody` | `src/main/modules/telemetry/exchanges/subscription/types/saleImportTopic.ts` |

### G4-a: Списание остатков после SUCCESS

Вызывается при `DrinkPreparingSuccess`:

```
ratio          = volume / dosage.drinkVolume
productWriteOff = dosage.product * ratio * concentrationRatio
waterMl        = round(dosage.water * ratio)

container.volume = max(0, container.volume - productWriteOff)
WATER_USAGE_ML  += waterMl

→ сохранить конфиг
→ sendCellVolumeImportFromConfig()   // cellVolumeImportTopic — все 6 ячеек
```

`concentrationRatio` должен быть сохранён до старта готовки (аналог `lastPreparedConcentrationRatio` в electron).

### G4-b: Продажа в телеметрию (`saleImportTopic`)

После успешной готовки отправить `saleImportTopic` — аналог вызова `sendSaleImportTopic` в payment-флоу электрона.

Структуру тела взять из `src/main/modules/telemetry/exchanges/subscription/types/saleImportTopic.ts`.  
Точка вызова: то же место, где уже отправляется событие успешной оплаты/продажи (если интегрирован payment в этапе C/D — подключить туда же).

### G4-c: Ручное редактирование остатков в сервисном меню

Вкладка «Наполнение» (или «Остатки» — по структуре electron `InventoryTab`):
- Поля для ввода объёма по каждому контейнеру.
- Кнопка «Сохранить» → `applyCellVolumes` → сохранить + `sendCellVolumeImportFromConfig`.

### DoD G4

- [ ] После `SUCCESS` в моке: объём контейнера уменьшается на правильное значение, логируется
- [ ] `cellVolumeImportTopic` отправляется в телеметрию (или логируется при offline)
- [ ] `saleImportTopic` отправляется после успешного напитка
- [ ] Вкладка остатков в сервисном меню: ручной ввод → сохранение → телеметрия
- [ ] Unit-тест: расчёт `productWriteOff`, `waterMl`

---

## Порядок запуска задач

| # | ID | Зависимость | moduleId для sessionId |
|---|---|---|---|
| 1 | G1 | — | `stage-g1-water-calibration` |
| 2 | G2 | — (параллельно G1) | `stage-g2-syrup-calibration` |
| 3 | G3 | G1 (нужен `flowRateMlPerSec`) | `stage-g3-cooking-flow` |
| 4 | G4 | G3 (нужен SUCCESS-event) | `stage-g4-inventory-telemetry` |

G1 и G2 можно запустить одновременно. G3 зависит от G1. G4 зависит от G3.

---

## Правила источников (наследуются из основного ТЗ)

- Протокол контроллера, команды, байтовые форматы — **wiva_electron** (`hardware/controller/**`).
- Телеметрия (структуры топиков, порядок вызовов) — **wiva_electron** (`modules/telemetry/**`).
- Транспорт WebSocket, регистрация — **legacy Android kiosk**.
- Сервисное меню: структура групп/вкладок — `ViwaServiceMenuStructure.kt`, вёрстка — `ServiceScreen.kt` + `service/tabs/*`.
