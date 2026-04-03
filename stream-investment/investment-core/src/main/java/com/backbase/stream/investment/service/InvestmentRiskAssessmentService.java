package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.investment.api.service.v1.model.Assessment;
import com.backbase.investment.api.service.v1.model.BaseAssessmentRequest;
import com.backbase.investment.api.service.v1.model.OASBaseAssessment;
import com.backbase.investment.api.service.v1.model.PatchedBaseAssessmentRequest;
import com.backbase.stream.configuration.IngestConfigProperties;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service wrapper around generated {@link RiskAssessmentApi} providing guarded create/patch operations with logging,
 * minimal idempotency helpers and consistent error handling.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Risk assessment creation and updates for clients</li>
 *   <li>Risk question creation and updates</li>
 * </ul>
 *
 * <p>Design notes (see CODING_RULES_COPILOT.md):
 * <ul>
 *   <li>No direct manipulation of generated API classes beyond construction & mapping</li>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying WebClient are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRiskAssessmentService {

    private final RiskAssessmentApi riskAssessmentApi;

    /**
     * Upserts multiple risk assessments for a client.
     *
     * <p>This method implements an upsert pattern for each assessment:
     * <ol>
     *   <li>Maps input assessments to API request objects</li>
     *   <li>For each assessment, searches for existing assessments by client UUID</li>
     *   <li>If found, patches the existing assessment</li>
     *   <li>If not found, creates a new assessment</li>
     * </ol>
     *
     * @param clientUuid  the UUID of the client
     * @param assessments the list of assessment requests to upsert (must not be null)
     * @return Flux emitting the created or updated assessments
     */
    public Mono<List<OASBaseAssessment>> upsertRiskAssessments(String clientUuid,
        List<BaseAssessmentRequest> assessments) {
        return Flux.fromIterable(Objects.requireNonNullElse(assessments, List.of()))
            .flatMap(assessmentRequest -> {
                log.debug("Upserting risk assessment for client: clientUuid={}", clientUuid);

                return upsertRiskAssessment(clientUuid, assessmentRequest)
                    .doOnSuccess(assessment -> log.debug(
                        "Successfully upserted risk assessment: uuid={}, clientUuid={}",
                        assessment.getUuid(), clientUuid))
                    .doOnError(throwable -> log.error(
                        "Failed to upsert risk assessment: clientUuid={}",
                        clientUuid, throwable));
            })
            .collectList();
    }

    /**
     * Upserts a single risk assessment for a client.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Searches for existing risk assessments for the client</li>
     *   <li>If found, patches the first matching assessment</li>
     *   <li>If not found, creates a new assessment</li>
     * </ol>
     *
     * @param clientUuid the UUID of the client (must not be null)
     * @param assessment the assessment request to upsert (must not be null)
     * @return Mono emitting the created or updated assessment
     * @throws NullPointerException if clientUuid or assessment is null
     */
    private Mono<OASBaseAssessment> upsertRiskAssessment(String clientUuid, BaseAssessmentRequest assessment) {
        Objects.requireNonNull(clientUuid, "Client UUID must not be null");
        Objects.requireNonNull(assessment, "Risk assessment must not be null");

        log.info("Upserting risk assessment for client: clientUuid={}", clientUuid);

        return listExistingRiskAssessments(clientUuid)
            .flatMap(existing -> patchRiskAssessment(clientUuid, existing, assessment))
            .switchIfEmpty(createNewRiskAssessment(clientUuid, assessment))
            .doOnSuccess(upserted -> log.info(
                "Successfully upserted risk assessment: uuid={}, clientUuid={}",
                upserted.getUuid(), clientUuid))
            .doOnError(throwable -> log.error(
                "Failed to upsert risk assessment: clientUuid={}",
                clientUuid, throwable));
    }

    /**
     * Lists existing risk assessments for a client.
     *
     * @param clientUuid the client UUID to search for
     * @return Mono emitting the first matching risk assessment, or empty if no match found
     */
    private Mono<Assessment> listExistingRiskAssessments(String clientUuid) {
        return riskAssessmentApi.getRiskAssessments(clientUuid, 1, null, null)
            .doOnSuccess(assessments -> log.debug(
                "List risk assessments query completed: clientUuid={}, found={} results",
                clientUuid, assessments != null ? assessments.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing risk assessments: clientUuid={}",
                clientUuid, throwable))
            .flatMap(assessments -> {
                if (Objects.isNull(assessments) || CollectionUtils.isEmpty(assessments.getResults())) {
                    log.info("No existing risk assessment found for clientUuid={}", clientUuid);
                    return Mono.empty();
                }

                int resultCount = assessments.getResults().size();
                if (resultCount > 1) {
                    log.warn("Found {} risk assessments for clientUuid={}, using first one",
                        resultCount, clientUuid);
                }

                Assessment existingAssessment = assessments.getResults().getFirst();
                log.info("Found existing risk assessment: uuid={}, clientUuid={}",
                    existingAssessment.getUuid(), clientUuid);
                return Mono.just(existingAssessment);
            });
    }

    /**
     * Creates a new risk assessment for a client.
     *
     * @param clientUuid the UUID of the client
     * @param assessment the assessment request to create
     * @return Mono emitting the newly created assessment
     */
    private Mono<OASBaseAssessment> createNewRiskAssessment(String clientUuid, BaseAssessmentRequest assessment) {
        log.info("Creating new risk assessment for client: clientUuid={}", clientUuid);

        return riskAssessmentApi.createRiskAssessment(clientUuid, assessment)
            .doOnSuccess(created -> log.info(
                "Successfully created risk assessment: uuid={}, clientUuid={}",
                created.getUuid(), clientUuid))
            .doOnError(throwable -> logRiskAssessmentError("create", clientUuid, throwable));
    }

    /**
     * Patches an existing risk assessment.
     *
     * @param clientUuid         the UUID of the client
     * @param existingAssessment the existing assessment to patch
     * @param assessment         the assessment data to update
     * @return Mono emitting the updated assessment
     */
    private Mono<OASBaseAssessment> patchRiskAssessment(String clientUuid, Assessment existingAssessment,
        BaseAssessmentRequest assessment) {
        UUID assessmentUuid = existingAssessment.getUuid();
        log.info("Patching risk assessment: uuid={}, clientUuid={}", assessmentUuid, clientUuid);
        log.debug("Patch risk assessment: uuid={}, object={}", assessmentUuid, assessment);

        PatchedBaseAssessmentRequest patchRequest = new PatchedBaseAssessmentRequest()
            .choices(assessment.getChoices())
            .status(assessment.getStatus())
            .flatAttributes(assessment.getFlatAttributes())
            .extraData(assessment.getExtraData());

        return riskAssessmentApi.patchRiskAssessment(clientUuid, assessmentUuid, patchRequest)
            .doOnSuccess(patched -> log.info(
                "Successfully patched risk assessment: uuid={}, clientUuid={}",
                patched.getUuid(), clientUuid))
            .doOnError(throwable -> logRiskAssessmentError("patch", clientUuid, throwable));
    }

    /**
     * Logs risk assessment errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param operation  the operation being performed (create/patch)
     * @param clientUuid the UUID of the client
     * @param throwable  the exception that occurred
     */
    private void logRiskAssessmentError(String operation, String clientUuid, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to {} risk assessment: clientUuid={}, status={}, body={}",
                operation, clientUuid, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to {} risk assessment: clientUuid={}", operation, clientUuid, throwable);
        }
    }

}