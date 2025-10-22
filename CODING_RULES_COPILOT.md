# Coding Rules for Copilot Agent

These rules reflect conventions observed in the Stream Services repository. The Copilot agent should default to them when generating or editing code.

## General Principles
- Prefer explicit, side-effect aware service methods with clear null checks (`Objects.requireNonNull`).
- Avoid mutating generated OpenAPI model classes outside of construction/mapping; encapsulate logic in services or mappers.
- Use Lombok for data classes (`@Builder`, `@Value`, `@Getter`, `@RequiredArgsConstructor`) and keep constructors simple.
- Leverage MapStruct for object mapping. Mapper interfaces should be small and annotated with `@Mapper(componentModel = "spring")` when DI is needed.
- Keep logging uniform: `log.info` for successful create/update operations, `log.debug` for read/list operations and patch attempts, `log.error` for failures including status & response body where available.
- Reactive style: Return `Mono`/`Flux` in service APIs; avoid blocking calls. Chain error handling with `onErrorResume` only for expected cases (e.g. 404 to empty) otherwise propagate.
- Idempotency: Perform existence checks only when necessary to avoid unnecessary round-trips.
- Write small, focused mappers; do not embed business logic inside MapStruct mapping methods.
- Favor constructor injection (Lombok `@RequiredArgsConstructor`) over field injection.
- Keep public APIs defensive: validate input early and fail fast with clear messages.

## Error Handling
- Distinguish between expected (e.g. 404 -> empty Mono) and unexpected errors (propagate or wrap).
- When catching `WebClientResponseException`, log status code and response body.
- Provide helper log methods (e.g. `logCreateError`, `logPatchError`) to avoid duplication.

## Logging Conventions
- Use structured placeholder logging: `log.info("Created entity id={}", id);`
- Avoid logging whole request/response bodies at info level; use debug for verbose payloads.
- Include key identifiers (UUIDs, external IDs) in all lifecycle operation logs.

## MapStruct Usage
- Declare a repository-wide property `mapstruct.version` to keep versions consistent.
- Use annotation processors via `maven-compiler-plugin` only; do not add MapStruct processor as a runtime dependency.
- For custom mapping requiring logic, prefer default methods in mapper interface rather than post-processing the target in services.

## Reactive Patterns
- Avoid `subscribe()` inside services; return publisher and let caller orchestrate.
- Use `switchIfEmpty` for conditional create flows.
- Keep side-effect logging inside reactive pipelines with `doOnSuccess` / `doOnError`.

## Testing & Build
- Support skipping tests via standard Maven flags: `-DskipTests` or `-Dmaven.test.skip=true` cascading to unit & integration tests.
- Keep integration tests named with `*IT.java`; unit tests should not include IT suffix.
- Provide pluginManagement configuration ensuring consistent surefire/failsafe versions across modules.

## Configuration Metadata
- Additional Spring configuration metadata JSON must only contain keys: `groups`, `hints`, `properties`.
- Do not introduce custom keys like `ignored`; instead document ignored properties in code comments.

## API Client Guidelines
- Wrap generated clients (e.g. `ClientApi`) in thin services adding logging, idempotency and error translation.
- Never leak internal/external correlation logic outside these service boundaries.

## Patch/Update Operations
- PATCH requests should send only fields intended to change (avoid full object copies that trigger validation 400s).
- Prefer PUT (update) when replacing entire resource state.

## File & Module Structure
- Keep module POMs lean; centralize shared version and plugin configs at the aggregator root.
- Avoid duplicate version declarations; rely on root properties.

## Naming
- Use clear operation constants (`OP_CREATE`, `RESULT_FAILED`) in saga/executor classes for consistency.
- Suffix domain tasks with `Task` and sagas with `Saga`.

## Generated Code
- Do not manually edit generated sources; extend via wrappers or mappers.
- Add generated sources through `build-helper-maven-plugin` rather than direct source directory reconfiguration.

## Security & Secrets
- Do not log secrets or tokens. Mask any potentially sensitive information.

## When Generating New Code
1. Define clear input/output contracts.
2. Add null and boundary validations first.
3. Provide minimal unit tests (happy path + one failure path).
4. Ensure reactive endpoints do not block.
5. Keep methods under ~40 lines; refactor helpers early.

These rules serve as the default baseline. Deviations should be documented in code comments or module README.

