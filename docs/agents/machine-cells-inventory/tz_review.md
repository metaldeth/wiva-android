# Ревью ТЗ: machine-cells-inventory (круг 2)

**Дата:** 2026-07-19  
**Ревьюер:** tz-reviewer  
**Круг:** 2 (повторная проверка после правок аналитика)  
**Артефакты:** `request.md`, `tz.md`, brief `FEATURE_MACHINE_CELLS_INVENTORY.md`, `tz_review.md` (круг 1)

---

## Краткий вывод

ТЗ **готово к передаче architect/planner**. Все **некритичные замечания круга 1 закрыты**. Постановка из `request.md` и ключевые разделы brief отражены полно: 9 юзер-кейсов с альтернативами, 7 рекомендуемых web UI-сценариев (не gate), 32 проверяемых критерия приёмки. Противоречий с явными ограничениями постановки **нет**.

**Критичных блокеров нет.**

---

## Критичные проблемы

*Отсутствуют.*

---

## Закрытие замечаний круга 1

| # | Замечание (круг 1) | Статус | Где закрыто в `tz.md` |
|---|-------------------|--------|------------------------|
| 1 | Конфликт SoT: dashboard PATCH vs LWW по `updatedAt` | ✅ Закрыто | Таблица Source of truth (§94): dashboard PATCH побеждает; LWW **только** между двумя machine content reports. OQ-8, UC-4 A1 — та же формулировка |
| 2 | Reconnect / server-initiated snapshot после hello | ✅ Закрыто | UC-1 A3; протокол downlink (§130); UC-6 A2; acceptance #16–17 |
| 3 | Неполная сводка OQ (OQ-7, 8, 10) | ✅ Закрыто | Таблица TBD включает OQ-1…OQ-10 |
| 4 | Несогласованность пути register | ✅ Закрыто | UC-1 и acceptance #1: `POST /api/v1/machines/register` |
| 5 | Customer drink list — слабое покрытие в acceptance | ✅ Закрыто | Acceptance #27: DrinkContainer из snapshot, не legacy merge |
| 6 | UC-7 A1 — merge policy при snapshot | ✅ Закрыто | UC-7 A1 (полный replace); NFR (§399–400); acceptance #25 |
| 7 | Опечатки «Автomat» | ✅ Закрыто | В документе не обнаружены |
| 8 | Dedup без отдельного UC | ℹ️ Без изменений | Покрыт UC-3 A1 + acceptance #7; отдельный UC не обязателен |

---

## Некритичные замечания (круг 2)

### 1. Терминология «snapshot merge» в разделе тестирования

В § «Тестирование» (строка ~429) для Android unit tests указано «snapshot **merge**», тогда как по всему ТЗ политика MVP — **полный replace** (`telemetryCellsSnapshot`). Смысл понятен developer'у, но для planner/architect лучше заменить на «snapshot apply / replace» — косметика, не блокер.

### 2. Brief OQ-8 vs уточнение в ТЗ

Brief §9 (OQ-8) по-прежнему формулирует «Last-write-wins по `updatedAt`» без явного приоритета dashboard. **ТЗ исправляет это корректно** и не противоречит постановке `request.md`. Architect может при необходимости синхронизировать brief с формулировкой ТЗ — вне scope ревью ТЗ.

---

## Сверка с чеклистом ревью

| Пункт | Статус |
|-------|--------|
| Все юзер-кейсы из постановки | ✅ Web cells + products CRUD, два uplink, register→schema, out of scope |
| Основной + альтернативные сценарии | ✅ UC-1…UC-9 с альтернативами (UC-8 — auth-сценарии) |
| Актёры | ✅ Таблица + per-UC |
| Критерии приёмки проверяемы | ✅ 32 пункта с конкретными API/поведением/E2E |
| Нет противоречий с постановкой | ✅ |
| UI-сценарии web (`browserTesting` не gate) | ✅ Явная пометка §344; WEB-1…WEB-7 достаточны |

---

## Покрытие постановки → UC

| Требование request.md | UC / раздел ТЗ |
|----------------------|----------------|
| Страница автомата: остатки, смена содержимого и цен | UC-6, WEB-3…WEB-7 |
| Volume-only uplink | UC-3 |
| Content uplink (+ volumes желательно) | UC-4 |
| Register без ячеек → schema reconcile | UC-1, UC-2 |
| Low-priority volume consistency | UC-3, NFR |
| Out of scope cups/water | § Вне scope, acceptance #29–30 |
| Flat cell fields | § Модель данных |
| База продуктов CRUD + tasteMediaKey allowlist | UC-5, WEB-1, acceptance #8–9, #18 |

---

## Итог круга 2

| Метрика | Значение |
|---------|----------|
| Критичные проблемы | 0 |
| Замечания круга 1 закрыты | 7/7 обязательных; 1 информационное (dedup UC) |
| Новые некритичные | 1 (терминология merge vs replace в §тестирование) |
| Рекомендация | **Передать architect/planner**; опционально: правка «snapshot merge» → «snapshot replace» в §тестирование |
