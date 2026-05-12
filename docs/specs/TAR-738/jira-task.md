# TAR-738: BE: update Streams investment component with investment-service-api 1.6 or latest

## Context

| Relationship | JIRA-ID | Title | Impact |
|--------------|---------|-------|--------|
| Parent | TAR-681 | BSJ: Extend portfolio product and deprecate fields in model portfolio | TAR-738 is a prerequisite; the parent's portfolio model extensions depend on the 1.6.0 API being available in investment-core |

---

## User Story

**As a** integration developer / bank implementer consuming Streams APIs
**I want to** use new features of the investment service for portfolio-model ingestion
**So that** I can use new fields and capabilities introduced in investment-service-api 1.6.0 in portfolio ingestion

---

## Description

The Streams investment module must be updated to use investment-service-api version 1.6.0. New API specifications must be generated and integrated into the investment module. All changes introduced by the new version must be applied and verified, with any issues resolved to achieve a passing build.

---

## Acceptance Criteria

### Build & Test Verification

- [ ] `mvn clean install` on the investment module runs successfully, with no tests skipped and no test failures

---

## Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Accessibility | N/A |
| Performance | N/A |
| Security | N/A |
| Internationalization | N/A |

---

## SLAs

N/A — no performance-sensitive functionality

---

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Existing test fixtures reference old model fields | Test fixtures must be updated to match the 1.6.0 model structure before tests can pass |

---

## Out of Scope

The following are explicitly **NOT** part of this ticket:

- Any modules outside of `investment-core` that reference `investment-service-api`
