# Copilot Instructions — Backbase Stream Services

## Project Basics

- **Java 21 / Spring Boot** multi-module Maven mono-repo.
- **Fully reactive** — use `Mono`/`Flux` (Project Reactor) for all service calls. Never use blocking calls in production code.
- Parent POM: `com.backbase.buildingblocks:backbase-parent:20.0.0`. BOM: `backbase-bom`.

## Core Architecture Pattern

Every ingestion domain uses the **Saga + StreamTask** pattern:

1. Create a `StreamTask` subclass wrapping the aggregate data with a `getData()` accessor.
2. Create a Saga class implementing `StreamTaskExecutor<T>` with `executeTask(T)` returning `Mono<T>`.
3. Chain saga steps via `.flatMap()`. Each step receives and returns the task.
4. Track progress with `task.info(entity, operation, result, externalId, internalId, message)` and failures with `task.error(...)`.

```java
// Canonical saga pattern — see LegalEntitySaga.executeTask()
@Override
public Mono<MyTask> executeTask(MyTask streamTask) {
    return stepOne(streamTask)
        .flatMap(this::stepTwo)
        .flatMap(this::stepThree);
}
```

## Code Style & Conventions

- **Lombok everywhere**: annotate classes with `@Slf4j`, `@RequiredArgsConstructor`, `@Data`. Do not write manual getters/setters/constructors when Lombok suffices. See `lombok.config` for project settings (`addConstructorProperties=true`).
- **MapStruct for model mapping**: define `@Mapper` interfaces to convert between DBS API models, stream models, and composition API models. Access via `Mappers.getMapper(MyMapper.class)` or Spring injection with `componentModel = "spring"`.
- **No component scanning**: modules wire beans via `@Configuration` + `@Import(...)` chains. When adding a new service bean, register it in the module's `*Configuration` class and `@Import` it where needed.
- **DBS clients extend `CompositeApiClientConfig`** with a `@ConfigurationProperties("backbase.communication.services.<name>")` prefix. All are assembled in `DbsApiClientsAutoConfiguration`.
- **Error handling in sagas**: catch `WebClientResponseException` specifically, log the response body, record in task history, then wrap in `StreamTaskException`.

```java
.onErrorResume(throwable -> {
    if (throwable instanceof WebClientResponseException wce) {
        task.error(ENTITY, OP, ERROR, extId, null, wce, wce.getResponseBodyAsString(), MESSAGE);
    } else {
        task.error(ENTITY, OP, ERROR, extId, null, throwable, throwable.getMessage(), MESSAGE);
    }
    return Mono.error(new StreamTaskException(task, throwable, MESSAGE));
})
```

## Generated Code — Do Not Edit

- `stream-dbs-clients` generates reactive WebClient wrappers from OpenAPI specs via `boat-maven-plugin`. **Never hand-edit files under `target/generated-sources/`.**
- Stream API models in `api/` and `stream-compositions/api/` are also generated. Edit the `.yaml` spec, not the Java output.

## Build Commands

```bash
# Fast full build (no tests)
mvn clean install -DskipTest -Dmaven.test.skip=true

# Single module with upstream dependencies
mvn clean install -pl stream-legal-entity/legal-entity-core -am

# Run a composition service locally
cd stream-compositions/services/legal-entity-composition-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Testing Patterns

- **JUnit 5 + Mockito + reactor-test**. Use `@ExtendWith(MockitoExtension.class)`.
- Mock DBS API clients (`@Mock LimitsServiceApi limitsApi`), stub with `when(...).thenReturn(Mono.just(...))`.
- Verify reactive chains with `StepVerifier` or `.block()` in tests.
- **Configuration tests** use `ApplicationContextRunner` to validate Spring wiring without a full app context:

```java
@SpringJUnitConfig
class MyConfigTest {
    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void configurationTest() {
        contextRunner
            .withBean(DbsWebClientConfiguration.class)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(DbsApiClientsAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(MyConfiguration.class)
            .run(context -> assertThat(context).hasSingleBean(MyService.class));
    }
}
```

## Key Directories

| What | Where |
|---|---|
| SDK framework (StreamTask, UnitOfWork) | `stream-sdk/stream-parent/stream-worker/` |
| DBS client configs | `stream-dbs-clients/src/main/java/.../clients/config/` |
| Legal Entity Saga (reference saga) | `stream-legal-entity/legal-entity-core/src/.../LegalEntitySaga.java` |
| Shared ingestion models | `stream-models/legal-entity-model/` |
| Composition services (deployable apps) | `stream-compositions/services/` |
| OpenAPI specs | `api/`, `stream-compositions/api/` |

## Skills Reference

Before generating or modifying code, **read the relevant skill file** from `.github/skills/` and follow its rules exactly. Each skill is authoritative for its domain.

| Task / Domain | Skill File to Read |
|---|---|
| Build commands, adding modules, POM hierarchy, versioning | `.github/skills/stream-build-and-modules/SKILL.md` |
| Creating or modifying a Saga / StreamTask | `.github/skills/stream-saga-development/SKILL.md` |
| Reactive `Mono`/`Flux` pipelines, error handling, parallel ops | `.github/skills/stream-reactive-patterns/SKILL.md` |
| Spring `@Configuration` / `@Import` / `@Bean` wiring | `.github/skills/stream-spring-wiring/SKILL.md` |
| Adding or configuring a DBS API client (`CompositeApiClientConfig`) | `.github/skills/stream-dbs-client-config/SKILL.md` |
| MapStruct mappers (`@Mapper`, `@Mapping`, model conversion) | `.github/skills/stream-mapstruct-mapping/SKILL.md` |
| Composition services (REST endpoints, integration APIs, events) | `.github/skills/stream-composition-services/SKILL.md` |
| Writing unit / integration tests (JUnit 5, Mockito, StepVerifier) | `.github/skills/stream-testing-patterns/SKILL.md` |

**Rules for code generation:**

1. **Identify** which skill(s) apply to the task being performed.
2. **Read** the matching `SKILL.md` file(s) in full before writing any code.
3. **Follow** every rule listed in the `## Rules` section of each skill — they override general conventions when they conflict.
4. When multiple skills apply (e.g., new saga + new DBS client + tests), read all relevant skill files.
5. Do **not** skip skill files for "simple" changes — even small edits must conform to the skill's patterns.

## PR Checklist

- Update `CHANGELOG.md` under the next version heading.
- Use `hotfix/` branch prefix for patch-level changes; standard branches bump MINOR.
- Support branches use `.x` suffix (e.g., `support/2.45.x`).

