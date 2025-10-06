# Stream Services – Coding Rules for Copilot Agent

These rules guide AI-assisted contributions to the `stream-services` multi-module Backbase platform codebase. They are derived from observed conventions in the existing repository (multi-module Maven `pom.xml`, Java 21, Spring (Boot) with Backbase building blocks, Lombok, Sagas, reactive/imperative blends, OpenAPI codegen via boat plugin).

---
## 1. Core Principles
1. Preserve existing architectural boundaries (module responsibilities, package naming, layering). 
2. Prefer minimal, targeted changes over broad refactors unless explicitly requested.
3. Never introduce unvetted external dependencies if functionality exists in Backbase SDK / BOM or standard JDK / Spring.
4. Maintain backward compatibility for public APIs (models, service contracts) unless a major version bump is coordinated.
5. Idempotency, observability, and secure handling of data are non-negotiable.

---
## 2. Project & Module Structure
- Root packaging: `com.backbase.stream`.
- Module types follow patterns:
  - `*-core`: Domain logic, services, sagas, integration with downstream APIs.
  - `*-http`: API adapters / controllers (REST clients or servers) – keep transport concerns here.
  - `*-task` / `*-bootstrap-task`: Initialization, bulk ingestion, bootstrap orchestration.
  - `stream-models`: Shared, versioned model artifacts grouped by domain (e.g., `legal-entity-model`, `portfolio-model`). Do **not** leak core service internals into model modules.
  - `stream-sdk`: Shared starter / parent modules – reuse conventions instead of re-implementing.
- When adding a module:
  1. Create under root with consistent naming.
  2. Add `<module>` entry to root `pom.xml` preserving ordering (group logically; do not reshuffle unrelated lines).
  3. Use parent `<parent>` referencing `stream-services` unless intentionally different.

---
## 3. Build & Dependency Management
- Java version: `21` (respect `<java.version>` property).
- Managed via Backbase BOM + parent (`backbase-parent`). Avoid hard-coding versions for artifacts already in dependencyManagement.
- Before adding a dependency: 
  - Search existing modules; reuse if possible.
  - If adding: omit `<version>` if covered by BOM.
  - If not in BOM: justify with comment in POM above the dependency.
- Use `provided` scope for Lombok only; do **not** include Lombok at runtime.
- Do not alter plugin versions defined in properties unless part of a coordinated upgrade.
- Generated sources: Respect locations defined by:
  - `${codegen.generated-sources-dir}`
  - `${annotations.generated-sources-dir}`
  - Never manually edit generated code – extend/wrap instead.

---
## 4. Language & Style
- Use Java 21 features judiciously (records, pattern matching) only where they reduce complexity without harming clarity or backward interop.
- Prefer `final` for fields and local variables not reassigned.
- Avoid wildcard imports; keep imports grouped: static imports (optional), then third-party, then internal.
- Method ordering (typical): public API methods → protected → private helpers.
- Line length target: 120 chars (soft limit). Wrap method chains sensibly.
- Avoid premature inheritance; compose over extend.

---
## 5. Logging
- Use Lombok `@Slf4j`; do **not** manually instantiate loggers.
- Parameterized logging: `log.debug("Ingesting product: {}", productId);` – never concatenate strings.
- Levels:
  - `trace`: Highly verbose internals; avoid unless diagnostic.
  - `debug`: Flow decisions, branching, external request payload summaries (sanitized).
  - `info`: High-level lifecycle events (start/end of saga step, batch processed counts).
  - `warn`: Recoverable issues (retry triggered, partial failures).
  - `error`: Terminal failures with context and correlation IDs.
- Never log secrets, PII, credentials, tokens, raw customer data.
- For batch operations: log aggregated metrics (counts, durations) not every record unless debug.

---
## 6. Error Handling & Exceptions
- Use domain-specific exception types rather than generic `RuntimeException`.
- Wrap lower-level client exceptions to avoid leaking implementation details upward.
- Provide actionable messages (what failed, identifier, next action if known).
- For saga steps: return clear status or propagate exception that upper orchestration can handle (retry/backoff).
- Avoid swallowing exceptions; if suppressed, log at least debug with reason.

---
## 7. Sagas & Orchestration Pattern
- Saga classes named `*Saga` and annotated with `@Slf4j` and optionally Spring stereotypes if injectable.
- Each saga method should be:
  - Idempotent: safe to rerun (check existence before create, use natural keys, correlation IDs).
  - Composable: small focused steps; factor common validation into private helpers.
- Document side effects and external systems touched in class-level Javadoc.
- Support partial failure recovery – design compensating actions where feasible.

---
## 8. Services & Domain Logic
- Keep service interfaces slim; expose intent-based methods (e.g., `ingestArrangements`, not `processList`).
- Avoid mixing HTTP client code inside services – delegate to client adapters or gateway classes.
- Use constructor injection (Lombok `@RequiredArgsConstructor`) – never field injection.
- Validate inputs early; fail fast with clear messages.

---
## 9. Configuration Management
- Configuration property classes placed in `configuration` package, named `*ConfigurationProperties` and annotated with `@ConfigurationProperties(prefix = "...")` and optionally `@Validated`.
- Provide defaults where safe; rely on environment variables for secrets (never commit secrets).
- Use primitive wrappers (`Integer`, `Long`) for optional props; primitives for required with default.
- Add JSR-380 annotations for constraints (e.g., `@NotNull`, `@Positive`).

---
## 10. Reactive vs Imperative
- If using Reactor (e.g., `Mono`, `Flux`) keep method signatures reactive end-to-end; avoid `.block()` in reactive chains.
- Segregate blocking I/O behind schedulers if bridging legacy clients.
- Don’t mix synchronous side effects inside reactive pipelines without appropriate operators.
- For non-reactive modules, remain imperative; do not introduce Reactor solely for stylistic reasons.

---
## 11. Models & DTOs
- Generated OpenAPI models (boat plugin output) should not be manually edited; extend or map.
- Keep separation between: 
  - External API DTOs
  - Internal domain models
  - Persistence/mapping layer representations (if any)
- Use mapper utilities or dedicated mapper classes (avoid scattering conversion logic).
- Avoid exposing internal IDs through external DTOs unless explicitly required.

---
## 12. Testing Strategy
- Framework: JUnit 5 (assumed), with Mockito / AssertJ style (confirm before introducing new libs).
- Test layers:
  - Unit: services, sagas (fast, isolated; mock external clients).
  - Integration: wiring of Spring context, configuration properties, external client stubs.
  - Contract / API: OpenAPI compatibility (regenerate and diff if necessary).
- Naming:
  - Test class: `<ClassUnderTest>Test`.
  - Methods: `should<Outcome>When<Condition>()` or BDD style.
- Cover edge cases: empty inputs, duplicates, retries, partial failures.
- Use deterministic data builders (Lombok builders or test data factories) – avoid randomization unless specifically testing entropy.
- No sleeps; use Reactor StepVerifier for reactive flows.

---
## 13. Performance & Scaling
- Batch operations: process in chunks; log progress every N items.
- Avoid N+1 remote calls – batch where API supports.
- Use streaming ingestion where large datasets; keep memory footprint predictable.
- Introduce caching only with explicit invalidation strategy.
- Guard loops with upper bounds; validate list sizes before processing.

---
## 14. Observability & Metrics
- Integrate with existing logging/metrics infrastructure (Micrometer if present). Check before adding.
- Expose meaningful counters/timers for ingestion steps (e.g., `products.ingested.count`).
- Correlate logs with request or batch IDs (use MDC if already standard in project – verify before adding).
- Avoid high-cardinality metric labels (no raw IDs).

---
## 15. Security & Compliance
- Never log or persist secrets, access tokens, raw personally identifiable data.
- Validate all external inputs – even if upstream validates.
- Sanitize external service errors before propagating.
- Principle of least privilege in access-control interactions.

---
## 16. API Design (if adding HTTP endpoints)
- Use REST resource naming; plural nouns where appropriate.
- Support pagination using existing platform conventions (look at other modules before inventing patterns).
- Return appropriate HTTP status codes (201 for creates, 202 for async acceptance, 409 for conflicts, 422 for semantic validation errors).
- Provide idempotency keys for POST ingestion endpoints when relevant.
- Document via OpenAPI; regenerate artifacts using configured boat/openapi plugin (don’t hand-craft mismatched annotations).

---
## 17. Versioning & Release Process
- Update `CHANGELOG.md` with Keep a Changelog format (Added / Changed / Fixed / Removed) when modifying module behavior.
- Use provided `set-version.sh` to bump versions consistently.
- Avoid snapshot dependencies unless part of a coordinated internal milestone.

---
## 18. Code Generation Guidelines
- Do not hand-edit code placed under `${project.build.directory}/generated-sources`.
- If customization needed: create wrapper / decorator / extension class in regular source tree.
- After updating OpenAPI specs: run maven generate phase and ensure new sources added via build-helper plugin.

---
## 19. Naming Conventions
- Classes: `PascalCase`; interfaces typically capability-based (e.g., `ArrangementService`).
- Constants: `UPPER_SNAKE_CASE` inside `final class` or interface only if purely constants.
- Packages: lowercase, domain-oriented (`product`, `portfolio`, `limit`, `configuration`).
- Methods: verbs expressing intent (`ingest`, `create`, `assign`, `fetch`).
- Avoid abbreviations unless ubiquitous (e.g., `ID`, not `Ident`).

---
## 20. Nullability & Defensive Coding
- Prefer non-null returns; return empty collections instead of `null`.
- Use `Optional` only for truly optional single values; don’t wrap collections in `Optional`.
- Annotate with JetBrains `@NotNull` / `@Nullable` where clarity is needed (the project imports `org.jetbrains:annotations`).
- Validate external DTOs (e.g., using Spring validation) before processing.

---
## 21. Migration & Refactoring Guidance
- For existing patterns that are inconsistent, prefer convergence toward the majority style – do not introduce a third variant.
- Large refactors require incremental commits with clear rationale; AI should avoid sweeping changes unless directed.

---
## 22. PR / Change Hygiene (AI-Specific)
When generating or modifying code:
1. Check for existing utilities before adding new helpers.
2. Preserve public method signatures unless feature explicitly changes contract.
3. Add/adjust tests for all new logic branches.
4. Run `mvn -q -DskipTests compile` locally to validate compilation (AI should simulate where possible).
5. Ensure no TODOs remain unless accompanied by GitHub issue reference.
6. Keep diffs minimal: no unrelated formatting or reorderings.

---
## 23. Do / Don’t Quick Table
| Do | Don’t |
|----|-------|
| Use constructor injection | Use field injection |
| Log with placeholders | String concatenate in logs |
| Add tests with new features | Leave untested branches |
| Respect module boundaries | Cross-import internal impl classes |
| Wrap external exceptions | Leak low-level client stack traces |
| Use existing BOM-managed versions | Hardcode duplicate versions |
| Idempotent saga steps | Assume single-run semantics |

---
## 24. AI Generation Guardrails
- If uncertain about an internal API, search repository before creating a new abstraction.
- Prefer reusing patterns from at least two existing modules for consistency.
- Flag (in comments) any assumption about external service behavior that isn’t codified yet.
- Never invent environment property names – confirm by searching for existing keys.
- Do not add persistence/storage mechanisms unless explicitly required by task.

---
## 25. Example Snippets
Service skeleton:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIngestionService {
    private final ExternalProductClient productClient;
    private final ArrangementService arrangementService;

    public void ingestProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            log.info("No products to ingest");
            return;
        }
        productIds.forEach(this::ingestSingle);
    }

    private void ingestSingle(String productId) {
        log.debug("Starting ingestion for product: {}", productId);
        ProductDto dto = productClient.fetch(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        arrangementService.ensureArrangement(dto);
        log.info("Completed ingestion for product: {}", productId);
    }
}
```
Configuration properties:
```java
@ConfigurationProperties(prefix = "stream.product.ingestion")
@Validated
@Data
public class ProductIngestionProperties {
    @NotNull
    private Integer batchSize = 100;

    @Positive
    private int maxRetries = 3;
}
```

---
## 26. When In Doubt
1. Search existing modules for analogous functionality.
2. Conform to dominant style.
3. Minimize surface area of change.
4. Add tests and logging for new critical paths.
5. Escalate (comment) rather than guess on domain logic.

---
## 27. Future Enhancements (Backlog – Do Not Auto-Implement)
- Centralized logging MDC correlation guidelines doc.
- Unified metrics naming conventions doc.
- Architectural decision records (ADR) per major pattern.

---
## 28. Acceptance Checklist for AI-Generated Changes
- [ ] Compiles with `mvn -q -DskipTests compile`.
- [ ] New/updated tests pass locally.
- [ ] No unmanaged dependency versions added.
- [ ] Logging levels appropriate.
- [ ] No secrets or PII logged.
- [ ] CHANGELOG updated if externally visible behavior changed.
- [ ] Module boundaries respected.
- [ ] Generated sources untouched.

---
These rules should be refined as the codebase evolves; treat this as a living document. PRs updating this guide should clearly state rationale.

