# Task 10 — Melanie — Reports screen + ViewModel (text-only)

**Owner:** Melanie
**Estimated effort:** ~1 hour
**Prerequisites:** `05-melanie-dashboard` (similar patterns)
**Day:** 3

## Goal

Text-based reports screen. No Vico, no charts (cut from scope). Three
cards: today's tons, today's spend, top-5 suppliers this month.

## Inputs

- `docs/Programa1/entrega/02-requerimientos/content.md` § 3.5 (RF-REP-01..04)
- `docs/Programa1/entrega/07-manual-de-usuario/content.md` § 7
- Repo: `PurchaseRepository.getTodaySummary()` + a custom in-VM aggregation for the month-by-supplier
- Utils: `MoneyFormatter`, `DateKey`

## Outputs

- `ui/reports/ReportsScreen.kt`:
  - Big card: "Toneladas de hoy" — value from `getTodaySummary().totalTons`
  - Big card: "Gasto de hoy" — `MoneyFormatter` over `totalSpendCentavos`; subtext "(N entradas sin precio)" if `purchasesWithoutPrice > 0`
  - List: "Top 5 proveedores del mes" — text rows like "1. Hernández y Hermanos — 12.3 t"
- `ui/reports/ReportsViewModel.kt`:
  - Combines `getTodaySummary(todayDateKey())` + a month-prefix query (e.g. `observeBy*` filtered locally by `dateKey.startsWith("2026-05-")`) grouped by `supplierId`, then sorted DESC, capped at 5.
  - Excludes `supplierId == UNREGISTERED_ID` from the top-5 ranking.

## Acceptance criteria

- [ ] Today's tons reflects all live purchases dated today.
- [ ] Today's spend excludes price-less purchases AND shows the count of those.
- [ ] Top-5 list ranks correctly and excludes UNREGISTERED.
- [ ] CP-03 partial (price-less rows behavior) passes.

## Pitfalls / notes

- **Month-prefix query:** Firestore doesn't support `startsWith` natively, but `dateKey >= "2026-05-" && dateKey < "2026-06-"` works perfectly as a range query on string. Use that.
- **Don't try to add a chart** — explicitly cut. If you finish ahead of schedule (haha), add Vico back as a stretch goal.
- **Empty state:** if no purchases this month, top-5 list shows "Sin datos para este mes."
- **Format:** "12.3 t" (1 decimal). Currency: "$15,300.00 MXN".
