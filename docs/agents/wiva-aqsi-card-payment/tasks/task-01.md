# task-01 — Карточный метод оплаты (модель + персистентность)

**Зависимости:** —

## Связь с юзер-кейсом

UC-3: выбор метода оплаты картой (PAX / aQsi) с сохранением между перезапусками.

## Что сделать

- Ввести sealed `CardPaymentMethod` (PAX / AQSI) с сериализацией в строки `"PAX"` / `"AQSI"` для хранилища.
- Добавить в `JsonStoreKeys` **только** ключ `CARD_PAYMENT_METHOD` (`"cardPaymentMethod"`).
- Интерфейс `CardPaymentMethodRepository`: чтение/запись выбранного метода; дефолт **PAX** при отсутствии значения.
- Реализация через существующий паттерн JsonStore / config repository (как SBP-настройки), без изменения запрещённых модулей.

## Точки изменения (ориентиры путей)

- `app/src/main/java/com/wiva/android/domain/model/CardPaymentMethod.kt` (новый)
- `app/src/main/java/com/wiva/android/domain/repository/CardPaymentMethodRepository.kt` (новый)
- `app/src/main/java/com/wiva/android/data/repository/CardPaymentMethodRepositoryImpl.kt` (новый)
- `app/src/main/java/com/wiva/android/data/local/db/JsonStoreKeys.kt` — добавление константы ключа

**Не трогать:** `PaymentMethod.kt`, `IntegrationsModule.kt`, `PaymentTerminalService.kt`, контроллер.

## Тест-кейсы (TDD / unit)

1. **Дефолт метода:** при отсутствии записи в хранилище `getSelected()` возвращает PAX.
2. **Round-trip AQSI:** `setSelected(AQSI)` затем `getSelected()` возвращает AQSI.
3. **Round-trip PAX:** после сохранения PAX чтение возвращает PAX.
4. **Некорректная строка в сторе:** при неизвестном значении — безопасный fallback на PAX (или согласованное поведение проекта зафиксировать тестом).
5. **Маппинг строк:** `"PAX"` / `"AQSI"` ↔ соответствующие варианты sealed-типа.

Инструменты: junit + mock/fake для слоя хранилища по образцу существующих тестов приложения.

## Критерий этапа (ТЗ)

A1.
