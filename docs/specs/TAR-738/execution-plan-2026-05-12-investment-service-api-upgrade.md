---
date: 2026-05-12T17:19:00Z
researcher: Roman Kniazevych
git_commit: e57781931efa48aaf236301e1b7125ebc77744bb
branch: TAR-738
repository: stream-services
jira_ticket: TAR-738
preceding_artifacts: [docs/specs/TAR-738/solution-design-2026-05-12-investment-service-api-upgrade.md]
topic: investment-service-api-upgrade
status: draft
last_updated: 2026-05-12
last_updated_by: Roman Kniazevych
---

# Execution Plan: TAR-738

| Ticket | TAR-738: Investment Service API Upgrade |
|--------|-------------------|
| Status | DRAFT |
| Solution Design | `docs/specs/TAR-738/solution-design-2026-05-12-investment-service-api-upgrade.md` |

---

## Overview

Upgrade the `investment-service-api` to version 1.6.0, resolve all resulting compilation errors in production and test code, and update the CHANGELOG to reflect the changes.

---

## Steps

---

### Step 1: Bump `investment-service-api.version` to 1.6.0 and verify codegen

- **Description:** Update the version property in `investment-core/pom.xml` to 1.6.0 and verify that `boat-maven-plugin` regenerates both API client packages without error; fix `enumNameMappings` if codegen fails
- **Files:** `stream-investment/investment-core/pom.xml`
- **Tests:** `mvn generate-sources` exits 0; both `com.backbase.investment.api.service.v1` and `com.backbase.investment.api.service.sync.v1` client classes are regenerated in `target/generated-sources/`
- **Depends:** None
- **Estimate:** S

---

### Step 2: Fix production source compilation errors

- **Description:** Run `mvn compile` and resolve all "cannot find symbol" errors in production source caused by changed model classes, renamed methods, or updated API client interfaces in the 1.6.0 generated code
- **Files:** Any subset of the following (determined by compiler output from Step 1):
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentSaga.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentAssetUniverseSaga.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentContentSaga.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentAssetUniverseService.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentRiskAssessmentService.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentClientService.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentPortfolioService.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/resttemplate/InvestmentRestAssetUniverseService.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/configuration/InvestmentClientConfig.java`
  - `stream-investment/investment-core/src/main/java/com/backbase/stream/configuration/InvestmentRestServiceApiConfiguration.java`
- **Tests:** `mvn compile` exits 0 with no compilation errors in production source
- **Depends:** Step 1
- **Estimate:** M

---

### Step 3: Fix test source compilation errors and verify full build

- **Description:** Run `mvn test-compile` and fix all fixture construction errors in test source files caused by changed model field names or constructors; then run `mvn clean install` to confirm the complete build passes with no skipped or failed tests
- **Files:** All affected `*Test.java` files under `stream-investment/investment-core/src/test/java/` (determined by compiler output; may include `InvestmentAssetUniverseSagaTest.java`, `InvestmentSagaTest.java`, `InvestmentContentSagaTest.java`, `InvestmentAssetUniverseServiceTest.java`, `InvestmentRiskAssessmentServiceTest.java`, `InvestmentClientServiceTest.java`, `InvestmentPortfolioServiceTest.java`, `InvestmentRestAssetUniverseServiceTest.java`)
- **Tests:** `mvn clean install` exits 0; zero tests skipped; zero test failures; all saga happy-path (`State.COMPLETED`) and error-path (`State.FAILED`) scenarios pass; all service Mockito/StepVerifier scenarios pass; all configuration context-wiring tests pass
- **Depends:** Step 2
- **Estimate:** M

---

### Step 4: Update CHANGELOG.md

- **Description:** Add an entry for the `investment-service-api` 1.6.0 upgrade under the next version heading in `CHANGELOG.md`, following the established pattern from prior upgrades
- **Files:** `CHANGELOG.md`
- **Tests:** `CHANGELOG.md` contains an entry referencing `investment-service-api` 1.6.0 under the latest (unreleased) version heading; entry follows the same wording style as prior upgrade entries (lines 30, 62)
- **Depends:** Step 3
- **Estimate:** S

---

## Execution Order

```
Step 1: Bump investment-service-api.version + verify codegen
  ↓
Step 2: Fix production source compilation errors
  ↓
Step 3: Fix test source compilation errors + verify full build
  ↓
Step 4: Update CHANGELOG.md
```

Linear sequence — each step depends on the previous. No parallelism opportunities: Steps 2 and 3 are gated by the compiler output of Step 1; Step 4 is gated on a clean build from Step 3.

Critical path: Step 1 → Step 2 → Step 3 → Step 4 (all steps are on the critical path).

---

## Commit Strategy

Each step = 1 commit with format:
`fix(TAR-738): step [N] - [description]`

| Step | Commit Message |
|---|---|
| Step 1 | `fix(TAR-738): step 1 - bump investment-service-api to 1.6.0` |
| Step 2 | `fix(TAR-738): step 2 - fix production source compilation for 1.6.0` |
| Step 3 | `fix(TAR-738): step 3 - fix test fixtures and verify full build` |
| Step 4 | `fix(TAR-738): step 4 - update changelog for investment-service-api 1.6.0` |

---

## Rollback Plan

Each committed step is a rollback point. To recover:

**If implementation fails at Step 1** (codegen fails and cannot be fixed):
1. Revert `pom.xml` to `investment-service-api.version` = `1.4.1`
2. Verify `mvn generate-sources` succeeds with 1.4.1
3. No other files affected

**If implementation fails at Step 2** (production compilation cannot be resolved):
1. `git revert <step-1-commit-hash>` to restore `pom.xml` to 1.4.1
2. `git clean -fd` to remove any partially modified files
3. Verify `mvn compile` passes with 1.4.1

**If implementation fails at Step 3** (test failures cannot be resolved):
1. `git revert <step-2-commit-hash> <step-1-commit-hash>` to restore all production source and `pom.xml`
2. `git clean -fd` to remove any partially modified test files
3. Verify `mvn clean install` passes with 1.4.1

**If implementation fails at Step 4** (CHANGELOG update is incorrect):
1. `git revert <step-4-commit-hash>` to restore `CHANGELOG.md`
2. Correct the entry and recommit

---

## Self-Check

- [x] All components from the solution design are decomposed into steps (version bump → production fixes → test fixes → changelog)
- [x] Each step is a single logical unit (single responsibility)
- [x] Each step has verifiable test scenarios (compiler/build exit codes + test pass criteria)
- [x] Each step leaves the codebase in a working (committable) state
- [x] Dependencies are explicit; no circular dependencies; parallelism opportunities identified (none — linear critical path)
- [x] No step exceeds L (4 hours): Step 1 = S, Step 2 = M, Step 3 = M, Step 4 = S
- [x] All steps map back to solution design sections (Section 4 Changes table)
- [x] Rollback plan is specific and actionable for each step
