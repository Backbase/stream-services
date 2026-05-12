---
date: 2026-05-12T18:05:00+03:00
reviewer: Roman Kniazevych
git_commit: a6612f7e343b6944924980770654913f308637c2
branch: TAR-738
repository: stream-services
jira_ticket: TAR-738
preceding_artifacts: [docs/specs/TAR-738/solution-design-2026-05-12-investment-service-api-upgrade.md, docs/specs/TAR-738/execution-plan-2026-05-12-investment-service-api-upgrade.md]
topic: investment-service-api-upgrade
review_type: code-review
status: draft
last_updated: 2026-05-12
last_updated_by: Roman Kniazevych
---

# TAR-738: Code Review Summary

**Date:** 2026-05-12
**Agent:** Implementation Agent
**Mode:** JUDGE
**Verdict:** ✅ APPROVED

---

## Files Reviewed

| File | Step | Change Type |
|------|------|-------------|
| `stream-investment/investment-core/pom.xml` | 1 | Version property bump (`1.4.1` → `1.6.0`) |
| `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentPortfolioService.java` | 2 | Constructor call updates + `listPortfolioProducts` signature update |
| `stream-investment/investment-core/src/test/java/com/backbase/stream/investment/service/InvestmentPortfolioAllocationServiceTest.java` | 3 | `InvestorModelPortfolio` constructor call updated to 7-arg signature |
| `stream-investment/investment-core/src/test/java/com/backbase/stream/investment/service/InvestmentPortfolioServiceTest.java` | 3 | `listPortfolioProducts` mock stubs updated to 13-arg signature (5 occurrences) |
| `CHANGELOG.md` | 4 | Added entry under `[10.2.0]` |

---

## Results

| Check | Status |
|-------|--------|
| Null/undefined handling | ✅ |
| Method size (≤24 lines) | ✅ |
| Single responsibility | ✅ |
| Naming conventions | ✅ |
| Documentation comments on public methods | ✅ |

---

## Blockers (if any)

None.

---

## Notes

### Null/undefined handling — ✅

The `null` arguments introduced in the two constructor calls (`InvestorModelPortfolio` at line 150–151 and `PortfolioProduct` at line 153–154) are intentional and correct. They represent new 1.6.0 API fields (`name`, `cashWeight`, `description` for `InvestorModelPortfolio`; `name`, `description`, `image`, `order`, `badge`, `productCategory` for `PortfolioProduct`) that are not used in this ingestion path. The existing guard (`if (modelUuid.isEmpty())`) prevents NPE on `modelUuid.get(0)`. The `listPortfolioProducts` call preserves its existing `null` guard via `Optional.ofNullable(products)` (line 414).

### Method size — ✅

No new methods were introduced. All changes are targeted line edits within existing methods (`upsertInvestmentProducts`, `listExistingPortfolioProducts`). The `upsertInvestmentProducts` method body spans ~80 lines — a pre-existing overgrowth not introduced by this ticket and outside its scope. No new violations were introduced.

### Single responsibility — ✅

No change to method or class boundaries. Each method retains its single clear purpose. The constructor-call adjustments do not alter any control-flow or responsibilities.

### Naming conventions — ✅

All identifiers introduced or retained follow Java conventions: `camelCase` for variables, `PascalCase` for classes, no new constants introduced. Mock variables in tests (`productList`, `emptyList`, `existingProduct`) are descriptive and consistent with existing test patterns.

### Documentation comments on public methods — ✅

No new public methods were added. All modified public methods (`upsertInvestmentProducts`, `listExistingPortfolioProducts`) retain their existing Javadoc unchanged. Pre-existing gaps (`upsertPortfolios`, `upsertDeposits`) are not introduced by this ticket.

### Pre-existing observations (non-blocking, out of scope for TAR-738)

| Observation | Location | Notes |
|---|---|---|
| `upsertInvestmentProducts` exceeds method size limit (~80 lines) | `InvestmentPortfolioService.java:115` | Pre-existing; no new logic added |
| `.retry(2)` followed by `.retryWhen(...)` on same chain | `InvestmentPortfolioService.java:508–509` | Pre-existing redundancy; `.retry(2)` and `.retryWhen()` should not compose on the same publisher |
| `upsertPortfolios` and `upsertDeposits` missing Javadoc | `InvestmentPortfolioService.java:76, 519` | Pre-existing gaps; not introduced by this ticket |

### Test counts

- `InvestmentPortfolioAllocationServiceTest.java`: 1 constructor call updated
- `InvestmentPortfolioServiceTest.java`: 5 mock stub signatures updated across 5 test methods
- All 8 affected test methods pass; full `mvn clean install` exits 0 with zero skipped/failed tests

---

## Verdict

**0 blockers.** All changes are minimal, mechanical, and correctly scoped to the 1.6.0 API signature adjustments. No new code smells or violations introduced. APPROVED — proceed to Architecture Review.
