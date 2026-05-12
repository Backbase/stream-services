---
date: 2026-05-12
researcher: Roman Kniazevych
git_commit: e57781931efa48aaf236301e1b7125ebc77744bb
branch: TAR-738
repository: stream-services
jira_ticket: TAR-738
preceding_artifacts: []
topic: "investment-service-api-upgrade"
tags: [research, codebase]
status: complete
last_updated: 2026-05-12
last_updated_by: Roman Kniazevych
---

# Research: Investment Service Api Upgrade

**Date**: 2026-05-12
**Researcher**: Roman Kniazevych
**JIRA Ticket**: TAR-738
**Git Commit**: e57781931efa48aaf236301e1b7125ebc77744bb
**Branch**: TAR-738
**Repository**: stream-services

## Research Question

What is the current state of the `investment-core` module that must be understood to upgrade `investment-service-api` from version 1.4.1 to 1.6.0?

## Summary

The `stream-investment/investment-core` module is the sole implementation module for investment data ingestion in Streams. It currently references `investment-service-api` version `1.4.1`, defined as the property `investment-service-api.version` at `stream-investment/investment-core/pom.xml:18`. The module uses the `boat-maven-plugin` 0.18.1 to generate both reactive (WebClient) and synchronous (RestTemplate) API clients from the YAML spec unpacked from the `investment-service-api` artifact. Generated model and API classes from both `com.backbase.investment.api.service.v1` and `com.backbase.investment.api.service.sync.v1` packages are extensively imported across saga and service classes. All test fixtures are constructed programmatically using fluent model builders (no resource JSON/YAML files). The CHANGELOG documents a consistent pattern of single-property version bumps for prior investment-service-api upgrades (1.3.0 → 1.4.1 at version 9.12.0).

## Detailed Findings

### Component 1: Module Structure
- Aggregator POM at `stream-investment/pom.xml:16-18` — declares single child module `investment-core`.
- Child module at `stream-investment/investment-core/pom.xml` — contains all configuration, codegen, and source code.
- Module registered in root reactor at `pom.xml:24`.
- Parent POM reference in aggregator: `stream-services` at version 10.2.0 (`stream-investment/pom.xml:5-9`).

### Component 2: API Version Property and Dependency Unpack
- Version property: `investment-service-api.version` = `1.4.1` at `stream-investment/investment-core/pom.xml:18`.
- `maven-dependency-plugin` unpack at `stream-investment/investment-core/pom.xml:75-88`:
  - groupId: `com.backbase.investment`
  - artifactId: `investment-service-api`
  - classifier: `api`, type: `zip`
  - outputDirectory: `${project.build.directory}/yaml`

### Component 3: Code Generation (boat-maven-plugin)
`boat-maven-plugin` version `0.18.1` (defined at `pom.xml:46`) with two executions at `stream-investment/investment-core/pom.xml:97-134`:

**Execution 1** — id: `generate-investment-service-api-code`, goal: `generate-webclient-embedded`:
- inputSpec: `${project.build.directory}/yaml/investment-service-api/investment-service-api-v${investment-service-api.version}.yaml`
- apiPackage: `com.backbase.investment.api.service.v1`
- modelPackage: `com.backbase.investment.api.service.v1.model`
- enumNameMappings: `Etc/GMT-12=ETC_GMT_1222`
- configOptions: `openApiNullable=false`, `useBeanValidation=false`

**Execution 2** — id: `generate-investment-service-rest-sync-api-code`, goal: `generate-rest-template-embedded`:
- Same inputSpec and enumNameMappings
- apiPackage: `com.backbase.investment.api.service.sync.v1`
- modelPackage: `com.backbase.investment.api.service.sync.v1.model`
- configOptions: `openApiNullable=false`, `useBeanValidation=false`

### Component 4: Generated API Client Usage in Configuration
`stream-investment/investment-core/src/main/java/com/backbase/stream/configuration/InvestmentClientConfig.java` wires 13 WebClient-based API clients:
- `clientApi` (line 85), `investmentProductsApi` (line 91), `portfolioApi` (line 97), `financialAdviceApi` (line 103), `assetUniverseApi` (line 109), `allocationsApi` (line 115), `investmentApi` (line 121), `contentApi` (line 127), `paymentsApi` (line 133), `portfolioTradingAccountsApi` (line 139), `currencyApi` (line 145), `riskAssessmentApi` (line 151), `asyncBulkGroupsApi` (line 157).

`stream-investment/investment-core/src/main/java/com/backbase/stream/configuration/InvestmentRestServiceApiConfiguration.java` wires 2 RestTemplate-based clients:
- `restContentApi` → `com.backbase.investment.api.service.sync.v1.ContentApi` (line 68)
- `restAssetUniverseApi` → `com.backbase.investment.api.service.sync.v1.AssetUniverseApi` (line 75)

### Component 5: Generated Model Usage in Sagas
`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentSaga.java`:
- `com.backbase.investment.api.service.v1.model.BaseAssessmentRequest` (lines 347, 349)
- `com.backbase.investment.api.service.v1.model.PortfolioList` (line 157)

`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentAssetUniverseSaga.java`:
- `com.backbase.investment.api.service.v1.model.AssetCategoryTypeRequest` (line 338)
- `com.backbase.investment.api.service.v1.model.MarketRequest` (line 182)
- `com.backbase.investment.api.service.v1.model.MarketSpecialDayRequest` (line 240)

### Component 6: Generated Model Usage in Services
`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentAssetUniverseService.java`:
- `AssetUniverseApi assetUniverseApi` injected at line 37
- Models: `AssetCategory` (sync, lines 419/427/444), `AssetCategoryType` (v1, lines 500-531), `AssetCategoryTypeRequest` (v1, lines 500-567), `Market` (v1, lines 47-85), `MarketRequest` (v1, lines 47-84), `MarketSpecialDay` (v1, lines 151-185), `MarketSpecialDayRequest` (v1, lines 151-219)

`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentRiskAssessmentService.java`:
- `RiskAssessmentApi riskAssessmentApi` injected at line 40
- Models: `Assessment` (v1, lines 112-164), `BaseAssessmentRequest` (v1, lines 58/146), `OASBaseAssessment` (v1, lines 57/146)

`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentClientService.java`:
- `ClientApi clientApi` injected at line 46
- Models: `ClientCreateRequest` (v1, lines 81/235), `OASClient` (v1, lines 160/193)

`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentPortfolioService.java`:
- `InvestmentProductsApi productsApi` (line 70), `PortfolioApi portfolioApi` (line 71), `PaymentsApi paymentsApi` (line 72), `PortfolioTradingAccountsApi portfolioTradingAccountsApi` (line 73)
- Models: `PortfolioList` (v1, lines 76/306), `PortfolioProduct` (v1, lines 112/393)

`stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/resttemplate/InvestmentRestAssetUniverseService.java`:
- `AssetCategory` (sync, lines 211/234/303), `AssetCategoryRequest` (sync, line 305), `OASAssetRequestDataRequest` (sync, lines 43/90/73/126)

### Component 7: Test Fixture Patterns
All test data is constructed programmatically — no `src/test/resources` directory exists.

Fluent setter pattern (most common):
```java
// stream-investment/investment-core/src/test/java/com/backbase/stream/investment/saga/InvestmentAssetUniverseSagaTest.java:306-307
new com.backbase.investment.api.service.v1.model.Currency().code("USD").name("US Dollar")

// InvestmentAssetUniverseSagaTest.java:390-391
new com.backbase.investment.api.service.v1.model.Market().code("NYSE").name("New York Stock Exchange")

// InvestmentAssetUniverseSagaTest.java:458-459
new com.backbase.investment.api.service.v1.model.MarketSpecialDay().market("NYSE").description("Thanksgiving")
```

Constructor with UUID pattern (sync models):
```java
// stream-investment/investment-core/src/test/java/com/backbase/stream/investment/service/resttemplate/InvestmentRestAssetUniverseServiceTest.java:64-65
new com.backbase.investment.api.service.sync.v1.model.Asset(syncUuid)
```

Models with multiple fields:
```java
// stream-investment/investment-core/src/test/java/com/backbase/stream/investment/service/InvestmentAssetUniverseServiceTest.java:97-101
new MarketRequest().code("NYSE").name("NYSE").sessionStart("09:30").sessionEnd("16:00")
```

### Component 8: Test Patterns (Saga and Service)
Standard JUnit 5 + Mockito + StepVerifier setup at `InvestmentAssetUniverseSagaTest.java:85-122`:
```java
@Mock private InvestmentAssetUniverseService assetUniverseService;
@BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }
```

Mock returns reactive types:
```java
// InvestmentAssetUniverseSagaTest.java:304-307
when(investmentCurrencyService.upsertCurrencies(anyList()))
    .thenReturn(Mono.just(List.of(new Currency().code("USD").name("US Dollar"))));
```

StepVerifier assertions — happy path (`InvestmentAssetUniverseSagaTest.java:158-160`):
```java
StepVerifier.create(saga.executeTask(task))
    .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
    .verifyComplete();
```

StepVerifier assertions — error path, saga returns FAILED task (`InvestmentAssetUniverseSagaTest.java:175-177`):
```java
StepVerifier.create(saga.executeTask(task))
    .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
    .verifyComplete();
```

Interaction verification: `verify(investmentCurrencyService, never()).upsertCurrencies(any());` (line 283)

### Component 9: Historical API Upgrade Pattern (CHANGELOG.md)
Prior investment-service-api upgrades documented in `CHANGELOG.md`:
- Line 62: version 9.6.0 — `use latest investment service api 1.3.0`
- Line 30: version 9.12.0 — `use latest investment service api 1.4.1`

Pattern: Each upgrade appears as a single CHANGELOG entry under a new minor version, consistent with the project's versioning convention (PRs to main auto-bump MINOR).

Other comparable API version bumps across modules: Legal Entity API 3.1.0 (version 8.6.0, line 108), transaction-manager v3.0.2 (version 8.2.0, line 128).

## Code References

- `stream-investment/investment-core/pom.xml:18` - investment-service-api.version property
- `stream-investment/investment-core/pom.xml:75-88` - maven-dependency-plugin unpack config
- `stream-investment/investment-core/pom.xml:97-115` - boat-maven-plugin webclient execution
- `stream-investment/investment-core/pom.xml:116-134` - boat-maven-plugin rest-template execution
- `stream-investment/pom.xml:16-18` - aggregator module declaration
- `pom.xml:24` - root reactor module entry
- `pom.xml:46` - boat-maven-plugin.version property
- `stream-investment/investment-core/src/main/java/com/backbase/stream/configuration/InvestmentClientConfig.java:85-157` - WebClient API bean factory
- `stream-investment/investment-core/src/main/java/com/backbase/stream/configuration/InvestmentRestServiceApiConfiguration.java:68-75` - RestTemplate API bean factory
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentSaga.java:157,347,349` - model usage in saga
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/saga/InvestmentAssetUniverseSaga.java:182,240,338` - model usage in saga
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentAssetUniverseService.java:37,47-567` - API client injection and model usage
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentRiskAssessmentService.java:40,57-164` - API client injection and model usage
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentClientService.java:46,81-235` - API client injection and model usage
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/InvestmentPortfolioService.java:70-73,76-393` - API client injection and model usage
- `stream-investment/investment-core/src/main/java/com/backbase/stream/investment/service/resttemplate/InvestmentRestAssetUniverseService.java:43-303` - sync model usage
- `stream-investment/investment-core/src/test/java/com/backbase/stream/investment/saga/InvestmentAssetUniverseSagaTest.java:85-307` - test setup and fixture patterns
- `stream-investment/investment-core/src/test/java/com/backbase/stream/investment/service/InvestmentAssetUniverseServiceTest.java:97-1527` - service test patterns
- `stream-investment/investment-core/src/test/java/com/backbase/stream/investment/service/resttemplate/InvestmentRestAssetUniverseServiceTest.java:64-65` - sync model test pattern
- `CHANGELOG.md:30,62` - prior investment-service-api version upgrades

## Architecture Documentation

- The module follows the standard Streams saga + StreamTask pattern: saga orchestrates, services wrap generated clients.
- API client generation uses two codegen paths: reactive (WebClient embedded) and synchronous (RestTemplate embedded) from the same YAML spec.
- The YAML spec filename is version-parameterized: `investment-service-api-v${investment-service-api.version}.yaml` — updating the version property automatically changes the unpacked YAML filename and regenerates all clients.
- 13 WebClient API beans and 2 RestTemplate API beans are wired in two separate `@Configuration` classes.
- The `enumNameMappings` entry (`Etc/GMT-12=ETC_GMT_1222`) is present in both codegen executions (`stream-investment/investment-core/pom.xml:107-109` and `:126-128`).

## Test Conventions

- **Framework**: JUnit 5 (`@ExtendWith(MockitoExtension.class)` or `MockitoAnnotations.openMocks(this)`), Mockito for mocking, `StepVerifier` for reactive assertion, AssertJ for state assertions.
- **Fixture construction**: All model objects built programmatically using fluent setters (`new Model().field(value)`) or constructors accepting UUID. No external JSON/YAML test resources exist.
- **Mock return values**: Service mocks return `Mono.just(...)` or `Mono.error(...)`. Saga tests use `Flux`/`Mono` stubs.
- **Assertion style**: `StepVerifier.create(...).assertNext(result -> assertThat(result.getState()).isEqualTo(...)).verifyComplete()`. Error paths assert `State.FAILED` and call `verifyComplete()` (sagas swallow errors via `onErrorResume`).
- **Interaction verification**: `verify(mock, never()).method(any())` used to confirm skipped paths.
- **Test organization**: `@Nested` inner classes with `@DisplayName` for grouping related scenarios.

## Open Questions

- Which specific model fields, enum values, or API operations were added/changed/removed in `investment-service-api` 1.5.0 and 1.6.0 relative to 1.4.1? This requires access to the published `investment-service-api` artifact at versions 1.5.0 and 1.6.0 (not available in the local repository).
- Does `investment-service-api` 1.6.0 introduce any new enum values that would require additional `enumNameMappings` entries in the boat-maven-plugin configuration?
