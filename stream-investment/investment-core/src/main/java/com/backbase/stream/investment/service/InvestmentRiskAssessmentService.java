package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.investment.api.service.v1.model.Assessment;
import com.backbase.investment.api.service.v1.model.BaseAssessmentRequest;
import com.backbase.investment.api.service.v1.model.BaseRiskChoice;
import com.backbase.investment.api.service.v1.model.BaseRiskChoiceRequest;
import com.backbase.investment.api.service.v1.model.BaseRiskQuestionRequest;
import com.backbase.investment.api.service.v1.model.OASBaseAssessment;
import com.backbase.investment.api.service.v1.model.OASRiskQuestion;
import com.backbase.investment.api.service.v1.model.PatchedBaseAssessmentRequest;
import com.backbase.investment.api.service.v1.model.PatchedBaseRiskChoiceRequest;
import com.backbase.investment.api.service.v1.model.PatchedBaseRiskQuestionRequest;
import com.backbase.investment.api.service.v1.model.RiskChoice;
import com.backbase.stream.investment.model.QuestionChoice;
import com.backbase.stream.investment.model.RiskQuestion;
import java.util.Collection;
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

    public Mono<List<RiskQuestion>> upsertRiskQuestions(List<RiskQuestion> riskQuestions) {
        return Flux.fromIterable(Objects.requireNonNullElse(riskQuestions, List.of()))
            .flatMap(questionRequest -> {
                log.debug("Upserting risk question: code={}, order={}",
                    questionRequest.getCode(), questionRequest.getOrder());

                return upsertRiskQuestion(questionRequest)
                    .doOnSuccess(question -> log.debug(
                        "Successfully upserted risk question: uuid={}, code={}, order={}",
                        question.getUuid(), question.getCode(), question.getOrder()))
                    .doOnError(throwable -> log.error(
                        "Failed to upsert risk question: code={}, order={}",
                        questionRequest.getCode(), questionRequest.getOrder(), throwable));
            })
//            .flatMap(this::upsertRiskQuestionsChoices)
            .map(rq -> rq.getChoices().stream().map(c -> new BaseRiskChoice()
                    .question(rq.getCode())
                    .code(c.getCode())
                    .order(c.getOrder())
                    .description(c.getDescription())
                    .score(c.getScore()))
                .toList() // Return the parent question after processing choices
            )
            .collectList()
            .flatMapIterable(list -> list.stream().flatMap(Collection::stream).toList()) // Flatten List<List<BaseRiskChoice>> to BaseRiskChoice
            .flatMap(this::upsertRiskChoice)
            .collectList()
            .map(l -> riskQuestions);
    }

    public Mono<BaseRiskChoice> upsertRiskQuestionsChoices(Flux<RiskQuestion> rq) {
        return
    }

    /*public Mono<BaseRiskChoice> upsertRiskQuestionsChoices(BaseRiskChoice riskChoice) {
        log.debug("Upserting risk choice: question={}, code={}, order={}",
            riskChoice.getQuestion(), riskChoice.getCode(), riskChoice.getOrder());
        return upsertRiskChoice(riskChoice.getQuestion(), choice)
            .map(c -> riskQuestion) // Return the parent question for downstream processing
            .doOnSuccess(question -> log.debug(
                "Successfully upserted risk choice: uuid={}, code={}, order={}",
                question.getUuid(), question.getCode(), question.getOrder()))
            .doOnError(throwable -> log.error(
                "Failed to upsert risk choice: question={}, code={}, order={}",
                riskQuestion.getCode(), choice.getCode(), choice.getOrder(), throwable));
            );
    }*/

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
     * Upserts a single risk question.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Searches for existing risk questions by code</li>
     *   <li>If found, patches the existing question</li>
     *   <li>If not found, creates a new question</li>
     * </ol>
     *
     * @param question the risk question request to upsert (must not be null)
     * @return Mono emitting the created or updated risk question
     * @throws NullPointerException if question is null
     */
    private Mono<RiskQuestion> upsertRiskQuestion(RiskQuestion question) {
        Objects.requireNonNull(question, "Risk question must not be null");

        String code = question.getCode();
        Integer order = question.getOrder();

        log.info("Upserting risk question: code={}, order={}", code, order);

        return listExistingRiskQuestions(code)
            .flatMap(existing -> patchRiskQuestion(existing.getUuid(), question))
            .switchIfEmpty(createNewRiskQuestion(question))
            .doOnSuccess(upserted -> log.info(
                "Successfully upserted risk question: uuid={}, code={}, order={}",
                upserted.getUuid(), upserted.getCode(), upserted.getOrder()))
            .doOnError(throwable -> log.error(
                "Failed to upsert risk question: code={}, order={}",
                code, order, throwable));
    }

    private Mono<BaseRiskChoice> upsertRiskChoice(BaseRiskChoice choice) {
        Objects.requireNonNull(choice, "Risk question choice must not be null");

        String code = choice.getCode();
        Integer order = choice.getOrder();

        log.info("Upserting risk question: question={}, code={}, order={}", choice.getQuestion(), code, order);

        return listExistingRiskChoices(choice.getQuestion(), code)
            .flatMap(existing -> patchRiskChoice(existing.getUuid(), choice))
            .switchIfEmpty(createNewRiskChoice(choice))
            .map(o -> choice)
            .doOnSuccess(upserted -> log.info(
                "Successfully upserted risk question: uuid={}, question={}, code={}, order={}",
                upserted.getUuid(), upserted.getQuestion(), upserted.getCode(), upserted.getOrder()))
            .doOnError(throwable -> log.error(
                "Failed to upsert risk question: code={}, order={}",
                code, order, throwable));
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
     * Lists existing risk questions by code.
     *
     * @param code the risk question code to search for
     * @return Mono emitting the first matching risk question, or empty if no match found
     */
    private Mono<OASRiskQuestion> listExistingRiskQuestions(String code) {
        return riskAssessmentApi.listRiskQuestions(100, null)
            .doOnSuccess(questions -> log.debug(
                "List risk questions query completed: code={}, found={} total results",
                code, questions != null ? questions.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing risk questions: code={}",
                code, throwable))
            .flatMap(questions -> {
                if (Objects.isNull(questions) || CollectionUtils.isEmpty(questions.getResults())) {
                    log.info("No existing risk question found with code={}", code);
                    return Mono.empty();
                }

                // Filter by code
                OASRiskQuestion matchingQuestion = questions.getResults().stream()
                    .filter(q -> code.equals(q.getCode()))
                    .findFirst()
                    .orElse(null);

                if (matchingQuestion == null) {
                    log.info("No existing risk question found with code={}", code);
                    return Mono.empty();
                }

                log.info("Found existing risk question: uuid={}, code={}",
                    matchingQuestion.getUuid(), code);
                return Mono.just(matchingQuestion);
            });
    }

    private Mono<RiskChoice> listExistingRiskChoices(String question, String code) {
        return riskAssessmentApi.listRiskChoices(100, null)
            .doOnSuccess(choices -> log.debug(
                "List risk questions query completed: question={}, code={}, found={} total results",
                question, code, choices != null ? choices.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing risk choices: question={}, code={}",
                question, code, throwable))
            .flatMap(choices -> {
                if (Objects.isNull(choices) || CollectionUtils.isEmpty(choices.getResults())) {
                    log.info("No existing risk choice found with question={}, code={}", question, code);
                    return Mono.empty();
                }

                // Filter by code
                RiskChoice matchingQuestion = choices.getResults().stream()
                    .filter(q -> code.equals(q.getCode()) && question.equals(q.getQuestion().getCode()))
                    .findFirst()
                    .orElse(null);

                if (matchingQuestion == null) {
                    log.info("No existing risk choice found with question={}, code={}", question, code);
                    return Mono.empty();
                }

                log.info("Found existing risk choice: uuid={}, question={}, code={}",
                    matchingQuestion.getUuid(), question, code);
                return Mono.just(matchingQuestion);
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
     * Creates a new risk question.
     *
     * @param question the risk question request to create
     * @return Mono emitting the newly created risk question
     */
    private Mono<RiskQuestion> createNewRiskQuestion(RiskQuestion question) {
        log.info("Creating new risk question: code={}, order={}",
            question.getCode(), question.getOrder());

        BaseRiskQuestionRequest request = new BaseRiskQuestionRequest()
            .code(question.getCode())
            .order(question.getOrder())
            .score(question.getScore())
            .description(question.getDescription());
        return riskAssessmentApi.createRiskQuestion(request)
            .map(baseRiskQuestion -> question)
            .doOnSuccess(created -> log.info(
                "Successfully created risk question: code={}, order={}",
                created.getCode(), created.getOrder()))
            .doOnError(throwable -> logRiskQuestionError("create",
                question.getCode(), question.getOrder(), throwable));
    }

    private Mono<BaseRiskChoice> createNewRiskChoice(BaseRiskChoice choice) {
        log.info("Creating new risk question: code={}, order={}",
            choice.getCode(), choice.getOrder());

        BaseRiskChoiceRequest choiceRequest = new BaseRiskChoiceRequest()
            .question(choice.getQuestion())
            .code(choice.getCode())
            .order(choice.getOrder())
            .suitable(choice.getSuitable())
            .description(choice.getDescription())
            .score(choice.getScore());
        return riskAssessmentApi.createRiskChoice(choiceRequest)
            .map(o -> choice)
            .doOnSuccess(created -> log.info(
                "Successfully created risk question: code={}, order={}",
                created.getCode(), created.getOrder()))
            .doOnError(throwable -> logRiskQuestionError("create",
                choice.getCode(), choice.getOrder(), throwable));
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
     * Patches an existing risk question.
     *
     * @param questionUuid the UUID of the question to patch
     * @param question     the question data to update
     * @return Mono emitting the updated risk question
     */
    private Mono<RiskQuestion> patchRiskQuestion(UUID questionUuid, RiskQuestion question) {
        log.info("Patching risk question: uuid={}, code={}", questionUuid, question.getCode());
        log.debug("Patch risk question: uuid={}, object={}", questionUuid, question);

        PatchedBaseRiskQuestionRequest patchRequest = new PatchedBaseRiskQuestionRequest()
            .code(question.getCode())
            .order(question.getOrder())
            .description(question.getDescription())
            .score(question.getScore());

        return riskAssessmentApi.patchRiskQuestion(questionUuid, patchRequest)
            .map(baseRiskQuestion -> question)
            .doOnSuccess(patched -> log.info(
                "Successfully patched risk question: uuid={}, code={}",
                questionUuid, patched.getCode()))
            .doOnError(throwable -> logRiskQuestionError("patch",
                question.getCode(), question.getOrder(), throwable));
    }

    private Mono<BaseRiskChoice> patchRiskChoice(UUID choiceUuid, BaseRiskChoice choice) {
        log.info("Patching risk choice: uuid={}, question={}, code={}", choiceUuid, choice.getQuestion(), choice.getCode());
        log.debug("Patch risk choice: uuid={}, object={}", choiceUuid, choice);

        PatchedBaseRiskChoiceRequest patchRequest = new PatchedBaseRiskChoiceRequest()
            .question(choice.getQuestion())
            .code(choice.getCode())
            .order(choice.getOrder())
            .description(choice.getDescription())
            .score(choice.getScore());

        return riskAssessmentApi.patchRiskChoice(choiceUuid, patchRequest)
            .map(o -> choice)
            .doOnSuccess(patched -> log.info(
                "Successfully patched risk choice: uuid={}, question={}, code={}",
                choiceUuid, choice.getQuestion(), patched.getCode()))
            .doOnError(throwable -> logRiskQuestionError("patch",
                choice.getCode(), choice.getOrder(), throwable));
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

    /**
     * Logs risk question errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param operation the operation being performed (create/patch)
     * @param code      the code of the risk question
     * @param order     the order of the risk question
     * @param throwable the exception that occurred
     */
    private void logRiskQuestionError(String operation, String code, Integer order, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to {} risk question: code={}, order={}, status={}, body={}",
                operation, code, order, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to {} risk question: code={}, order={}", operation, code, order, throwable);
        }
    }
}