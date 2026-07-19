# task-04 — code review, круг 2 (OQ-2 re-key fix)

**Reviewer:** code-reviewer-complex  
**Date:** 2026-07-19  
**Round:** 2 (re-check после developer fix + content preserve hotfix)  
**Repo:** `wiva-telemetry`  
**Task:** `docs/agents/machine-cells-inventory/tasks/task-04.md`  
**Previous review:** круг 1 — `CONDITIONAL APPROVE`, critical C-1 (P2002 на re-key)  
**Test report:** `task-04-test-report.md`  
**Architecture:** C-1, C-3, C-5, reconcile §5 FEATURE

## Verdict

**APPROVE** — critical OQ-2 re-key fix **закрыт**. Hard-delete + insert в одной транзакции обходит `@@unique([machineId, cellNumber])`; mock эмулирует unique; volume/product/prices/contentSource переносятся со старой ячейки на новую. Happy path task-04 готов к task-05 (WS wiring).

---

## Focus check (круг 2)

| # | Проверка | Статус | Доказательство |
|---|----------|--------|----------------|
| 1 | Нет P2002 на `unique(machineId, cellNumber)` при re-key | ✅ | `delete` старой строки **до** `create` (стр. 49–55, 57–73); soft-deactivate на тот же slot больше не используется |
| 2 | Re-key сохраняет volume/product/prices (architecture invariant по slot) | ✅ | `rekeySource` → `volume`, `productId`, `dosage1Price`, `dosage2Price`, `contentSource`, `schemaRevision+1` (стр. 65–71); тест re-key: volume=800, product/prices/contentSource сохранены |
| 3 | Mock эмулирует unique | ✅ | `seedCell` + `create` в `machine-cells-store.ts`: конфликт `(machineId, cellNumber)` → throw с `code: 'P2002'` |
| 4 | L-3 inactive + повторный insert (edge) | ℹ️ Low | Не happy path; при `isActive=false` строка остаётся в БД с тем же `(machineId, cellNumber)` — повторный report с тем же uuid/cellNumber может дать PK/unique conflict. Для MVP/task-05 не блокер |

---

## Acceptance criteria (task-04)

| # | Критерий | Круг 1 | Круг 2 | Комментарий |
|---|----------|--------|--------|-------------|
| 1 | Reconcile insert: N cells, volume=0, product=null | ✅ | ✅ | без изменений |
| 2 | Reconcile preserve: repeat uuids → content unchanged | ✅ | ✅ | `buildStructuralUpdate` |
| 3 | Reconcile deactivate: missing → `isActive=false` | ✅ | ✅ | финальный цикл |
| 4 | Reconcile re-key: same cellNumber, new uuid | ⚠️ | ✅ | hard-delete + insert + content preserve |
| 5 | Volume apply: только volume (+ optional block/sos) | ✅ | ✅ | |
| 6 | Content MACHINE: product + prices + volume | ✅ | ✅ | |
| 7 | Content DASHBOARD gate | ✅ | ✅ | |
| 8 | Dedup: повторный messageId → false | ✅ | ✅ | |
| 9 | Snapshot: products[] + denormalized cells[] | ✅ | ✅ | |
| 10 | shouldPushSnapshot | ✅ | ✅ | |
| — | Export services из MachineCellsModule | ✅ | ✅ | |
| — | No WS send / no AppModule wiring | ✅ | ✅ | |

---

## Critical C-1 (круг 1) — resolution

### Было

Soft deactivate (`isActive=false`) + `create` с тем же `cellNumber` → P2002 на реальной Prisma/PostgreSQL (`@@unique` без partial index по `isActive`). Mock не ловил конфликт.

### Стало

```typescript
// cell-schema-reconcile.service.ts — re-key branch
await tx.machineCell.delete({ where: { id: matchedByCellNumber.id } });
rekeySource = matchedByCellNumber;
// ...
await tx.machineCell.create({ data: { /* content from rekeySource */ } });
```

- Unique slot освобождается физическим delete.
- Content переносится через `rekeySource` (hotfix после круга 2 developer).
- Unit-тест явно проверяет preserve + отсутствие старой строки (`store.cells.has(oldUuid) === false`).

**Статус:** ✅ **closed**

---

## Architecture compliance

C-1 / C-3 / C-5 — без изменений относительно круга 1; re-key больше не блокирует reconcile invariant «не сбрасывать content на том же физическом slot при замене контроллера».

| Invariant | Re-key (круг 2) |
|-----------|-----------------|
| uuid match → preserve content | ✅ `buildStructuralUpdate` |
| same cellNumber, new uuid → new row, content preserved | ✅ `rekeySource` |
| missing → soft deactivate | ✅ (не re-key path) |
| schema report не массово сбрасывает product/prices | ✅ |

---

## Findings (круг 2)

### Critical

_Нет._

---

### Medium (carry-over из круга 1)

#### M-1. `schemaHash`: golden vector в contract

Порядок ключей зафиксирован в JSDoc (`cellNumber, maxVolume, uuid`); cross-platform golden hex в `docs/contracts/` — по-прежнему желателен до Android codec. Не блокер task-05.

#### M-2. `buildSnapshot`: `product.findMany` без tenant scope

MVP single-tenant — OK; зафиксировать в contract при multi-tenant.

---

### Low / Info

| ID | Topic | Detail |
|----|-------|--------|
| L-1 | Reconcile `deactivated` count на re-key | Re-key инкрементирует `deactivated` при hard-delete — семантика ack «deactivated» vs «replaced»; косметика |
| L-2 | Duplicate cellNumber/uuid во входящем report | Валидация — task-05 WS handler |
| L-3 | Inactive cell reappears (same uuid/cellNumber) | Edge case hardware churn; inactive row в БД блокирует insert (PK/unique). Не ломает happy path |
| L-4 | Re-key: `blockVolume`/`sosVolume` fallback | При uuid-match: `incoming ?? existing`; при re-key create: `incoming ?? DEFAULT_*` — если optional поля отсутствуют в report, structural block/sos на re-key сбросятся в 0, не из `rekeySource`. Тест покрывает явные значения в fixture |
| L-5 | JSDoc re-key | Комментарий «insert a fresh row» не упоминает content preserve — обновить при следующем touch |
| L-6–L-7 | Dedup TTL, NoOp facade | Без изменений; task-05 |

---

## Файлы (delta круг 2)

| File | Круг 1 | Круг 2 |
|------|--------|--------|
| `cell-schema-reconcile.service.ts` | ⚠️ P2002 + content reset | ✅ delete+insert, `rekeySource` preserve |
| `test/helpers/machine-cells-store.ts` | ⚠️ no unique | ✅ P2002 emulation |
| `test/cell-schema-reconcile.service.spec.ts` | ⚠️ misleading re-key | ✅ preserve assertions + unique-safe path |

---

## Тесты

| Suite | Cases | Result | Замечание |
|-------|-------|--------|-----------|
| `cell-schema-reconcile.service.spec.ts` | 4/4 | PASS | re-key: delete, preserve content, unique-safe |
| Остальные task-04 suites | per test report | PASS | |
| `npm test` (локально, 2026-07-19) | — | PASS | reconcile suite включён |

---

## Scope notes (OK for task-04)

- `MachineCellsModule` не в `AppModule` — task-05.
- WS dispatch, REST cells controller — вне scope.
- `MACHINE_WS_PUSH_FACADE` = NoOp — корректно.

---

## Рекомендации для task-05

1. ~~Blocker re-key vs unique~~ — **снят**.
2. Golden `schemaHash` в contract + Android codec (M-1).
3. WS handler: dedup → apply chain; validation дубликатов cellNumber/uuid (L-2).
4. Опционально: re-key fallback `blockVolume`/`sosVolume` из `rekeySource` (L-4); политика L-3 (reactivate inactive vs upsert).

---

## Итог для оркестратора

| Field | Value |
|-------|-------|
| `hasCriticalIssues` | **false** |
| Critical C-1 (P2002) | **closed** |
| Content preserve on re-key | **closed** |
| Mock unique enforcement | **closed** |
| Tests | 4/4 reconcile PASS; full `npm test` PASS |
| Architecture C-1/C-3/C-5 | compliant |
| Ready for task-05 WS wiring | **yes** |
