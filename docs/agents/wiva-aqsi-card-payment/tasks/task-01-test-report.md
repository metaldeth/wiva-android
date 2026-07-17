# task-01 — отчёт по тестам

**Сессия:** `wiva-aqsi-card-payment`  
**Дата:** 2026-05-12  

## Команды

```bat
gradlew.bat :app:testDebugUnitTest --tests "com.wiva.android.data.repository.CardPaymentMethodRepositoryTest"
gradlew.bat assembleDebug
```

## Результат

| Шаг | Статус |
|-----|--------|
| `:app:testDebugUnitTest` (фильтр на `CardPaymentMethodRepositoryTest`) | OK |
| `assembleDebug` | OK |

## Покрытые тест-кейсы из task-01

| # | Описание | Тест |
|---|-----------|------|
| 1 | Дефолт PAX при отсутствии записи | `getSelected_whenMissing_returnsPax` |
| 2 | Round-trip AQSI | `roundTrip_aqsi` |
| 3 | Round-trip PAX | `roundTrip_pax` |
| 4 | Некорректная строка в сторе → fallback PAX | `getSelected_whenUnknownStored_returnsPax` |
| — | Пустая строка `""` в сторе → fallback PAX | `getSelected_whenEmptyStringStored_returnsPax` |
| 5 | Маппинг `"PAX"` / `"AQSI"` ↔ тип | `storageMapping_roundTrip_strings`, `fromStorageString_caseInsensitivePaxAndAqsi` |

## Примечания

- Хранилище в тестах — in-memory `FakeConfigRepository`, реализующий контракт `ConfigRepository` (как при работе через Room JsonStore).
- Доп. кейс после ревью: явно сохранённая пустая строка в сторе трактуется как неизвестное значение → дефолт PAX (`getSelected_whenEmptyStringStored_returnsPax`).
