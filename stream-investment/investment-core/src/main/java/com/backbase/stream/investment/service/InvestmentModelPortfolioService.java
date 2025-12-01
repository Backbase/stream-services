
package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.OASAssetModelPortfolioRequestRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioRequestDataRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.stream.investment.InvestmentData;
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
 * Service wrapper around generated {@link PortfolioApi} and {@link InvestmentProductsApi} providing guarded
 * create/patch operations with logging, minimal idempotency helpers and consistent error handling.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Investment product creation and updates (portfolio products)</li>
 *   <li>Investment portfolio creation and updates</li>
 *   <li>Client-to-portfolio associations via legal entity mappings</li>
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
public class InvestmentModelPortfolioService {

    private final FinancialAdviceApi financialAdviceApi;
    private final CustomIntegrationApiService customIntegrationApiService;

    public Flux<OASModelPortfolioResponse> upsertModels(InvestmentData investmentData) {
        return Flux.fromIterable(Objects.requireNonNullElse(investmentData.getModelPortfolioTemplates(), List.of()))
            .flatMap(modelPortfolioTemplate -> {
                log.debug("Upserting investment portfolio model: name={}, riskLevel={}",
                    modelPortfolioTemplate.getName(), modelPortfolioTemplate.getRiskLevel());

                OASModelPortfolioRequestDataRequest modelPortfolioRequest = new OASModelPortfolioRequestDataRequest()
                    .name(modelPortfolioTemplate.getName())
                    .riskLevel(modelPortfolioTemplate.getRiskLevel())
                    .allocation(modelPortfolioTemplate.getAllocations().stream()
                        .map(m -> new OASAssetModelPortfolioRequestRequest()
                            .asset(m.asset().getMap())
                            .weight(m.weight()))
                        .toList())
                    .cashWeight(modelPortfolioTemplate.getCashWeight());

                return upsertModelPortfolio(modelPortfolioRequest)
                    .doOnSuccess(modelPortfolio -> {
                        modelPortfolioTemplate.uuid(modelPortfolio.getUuid());
                        log.debug(
                            "Successfully upserted investment portfolio model: modelUuid={}, name={}, riskLevel={}",
                            modelPortfolio.getUuid(), modelPortfolio.getName(), modelPortfolio.getRiskLevel());
                    })
                    .doOnError(throwable -> log.error(
                        "Failed to upsert investment portfolio model: name={}, riskLevel={}",
                        modelPortfolioRequest.getName(), modelPortfolioRequest.getRiskLevel(), throwable));
            });

    }

    /**
     * Upserts a model portfolio via the Financial Advice API.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Searches for existing model portfolios by name and risk level</li>
     *   <li>If found, returns the existing model portfolio</li>
     *   <li>If not found, creates a new model portfolio</li>
     * </ol>
     *
     * @param modelPortfolio the model portfolio to upsert (must not be null)
     * @return Mono emitting the created or existing model portfolio
     * @throws NullPointerException if modelPortfolio is null
     */
    private Mono<OASModelPortfolioResponse> upsertModelPortfolio(OASModelPortfolioRequestDataRequest modelPortfolio) {
        Objects.requireNonNull(modelPortfolio, "InvestorModelPortfolio must not be null");

        String modelName = modelPortfolio.getName();
        Integer riskLevel = modelPortfolio.getRiskLevel();

        log.info("Upserting model portfolio: name={}, riskLevel={}", modelName, riskLevel);

        return listExistingModelPortfolios(modelName, riskLevel)
            .flatMap(pm -> patchModelPortfolio(pm.getUuid(), modelPortfolio))
            .switchIfEmpty(createNewModelPortfolio(modelPortfolio))
            .doOnSuccess(upserted -> log.info(
                "Successfully upserted model portfolio: uuid={}, name={}, riskLevel={}",
                upserted.getUuid(), upserted.getName(), upserted.getRiskLevel()))
            .doOnError(throwable -> log.error(
                "Failed to upsert model portfolio: name={}, riskLevel={}",
                modelName, riskLevel, throwable));
    }

    /**
     * Lists existing model portfolios by name and risk level.
     *
     * @param name      the model portfolio name to search for
     * @param riskLevel the risk level to filter by
     * @return Mono emitting the first matching model portfolio, or empty if no match found
     */
    private Mono<OASModelPortfolioResponse> listExistingModelPortfolios(String name, Integer riskLevel) {
        return financialAdviceApi.listModelPortfolio(
                null, null, null, 1, name, null, null, null, riskLevel, null)
            .doOnSuccess(models -> log.debug(
                "List model portfolios query completed: name={}, riskLevel={}, found={} results",
                name, riskLevel, models != null ? models.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing model portfolios: name={}, riskLevel={}",
                name, riskLevel, throwable))
            .flatMap(models -> {
                if (Objects.isNull(models) || CollectionUtils.isEmpty(models.getResults())) {
                    log.info("No existing model portfolio found with name={}, riskLevel={}", name, riskLevel);
                    return Mono.empty();
                }

                int resultCount = models.getResults().size();
                if (resultCount > 1) {
                    log.warn("Found {} model portfolios with name={} and riskLevel={}, using first one",
                        resultCount, name, riskLevel);
                }

                OASModelPortfolioResponse existingModel = models.getResults().get(0);
                log.info("Found existing model portfolio: uuid={}, name={}, riskLevel={}",
                    existingModel.getUuid(), name, riskLevel);
                return Mono.just(existingModel);
            });
    }

    /**
     * Creates a new model portfolio via the Financial Advice API.
     *
     * @param modelPortfolio the model portfolio to create
     * @return Mono emitting the newly created model portfolio
     */
    private Mono<OASModelPortfolioResponse> createNewModelPortfolio(
        OASModelPortfolioRequestDataRequest modelPortfolio) {

        log.info("Creating new model portfolio: name={}, riskLevel={}",
            modelPortfolio.getName(), modelPortfolio.getRiskLevel());
        return customIntegrationApiService.createModelPortfolioRequestCreation(null, null, null,
                modelPortfolio, null)
            .doOnSuccess(created -> log.info(
                "Successfully created model portfolio: uuid={}, name={}, riskLevel={}",
                created.getUuid(), created.getName(), created.getRiskLevel()))
            .doOnError(throwable -> logModelPortfolioError("create",
                modelPortfolio.getName(), modelPortfolio.getRiskLevel(), throwable));
    }

    private Mono<OASModelPortfolioResponse> patchModelPortfolio(UUID uuid,
        OASModelPortfolioRequestDataRequest modelPortfolio) {

        log.info("Patch model portfolio: name={}, riskLevel={}",
            modelPortfolio.getName(), modelPortfolio.getRiskLevel());
        log.debug("Patch model portfolio: iiud={}, object={}", uuid, modelPortfolio);
        return customIntegrationApiService.patchModelPortfolioRequestCreation(uuid.toString(),
                null, null, null, modelPortfolio, null)
            .doOnSuccess(created -> log.info(
                "Successfully patched model portfolio: uuid={}, name={}, riskLevel={}",
                created.getUuid(), created.getName(), created.getRiskLevel()))
            .doOnError(throwable -> logModelPortfolioError("patch",
                modelPortfolio.getName(), modelPortfolio.getRiskLevel(), throwable));
    }

    /**
     * Logs model portfolio creation errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param name      the name of the model portfolio being created
     * @param riskLevel the risk level of the model portfolio being created
     * @param throwable the exception that occurred during creation
     */
    private void logModelPortfolioError(String request, String name, Integer riskLevel, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to {} model portfolio: name={}, riskLevel={}, status={}, body={}",
                request, name, riskLevel, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to {}} model portfolio: name={}, riskLevel={}", request,
                name, riskLevel, throwable);
        }
    }


    /**
     * Logs portfolio creation errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param externalId the external ID of the portfolio being created
     * @param name       the name of the portfolio being created
     * @param throwable  the exception that occurred during creation
     */
    private void logPortfolioCreationError(String externalId, String name, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to create investment portfolio: externalId={}, name={}, status={}, body={}",
                externalId, name, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to create investment portfolio: externalId={}, name={}",
                externalId, name, throwable);
        }
    }

    /**
     * Logs portfolio product creation errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param productType the product type being created
     * @param throwable   the exception that occurred during creation
     */
    private void logPortfolioProductCreationError(String productType, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to create portfolio product: productType={}, status={}, body={}",
                productType, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to create portfolio product: productType={}", productType, throwable);
        }
    }

}
