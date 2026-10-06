package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.model.AssetModelPortfolio;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.model.PaginatedExpandedModelPortfolioList;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import com.backbase.stream.investment.service.resttemplate.RestTemplateModelPortfolioMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service wrapper around generated {@link FinancialAdviceApi} providing guarded create/patch operations with logging,
 * minimal idempotency helpers and consistent error handling.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Investment portfolio model creation and updates</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying WebClient are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentModelPortfolioService {

    private static final double WEIGHT_SUM_TOLERANCE = 1e-6;

    private final FinancialAdviceApi financialAdviceApi;
    private final InvestmentRestModelPortfolioService investmentRestModelPortfolioService;
    private final IngestConfigProperties config;
    private final RestTemplateModelPortfolioMapper modelPortfolioMapper =
        Mappers.getMapper(RestTemplateModelPortfolioMapper.class);

    public Flux<OASModelPortfolioResponse> upsertModels(InvestmentData investmentData) {
        return Flux.fromIterable(Objects.requireNonNullElse(investmentData.getModelPortfolios(), List.of()))
            .flatMap(modelPortfolioTemplate -> upsertModelPortfolioResponse(modelPortfolioTemplate)
                .doOnSuccess(response -> modelPortfolioTemplate.uuid(response.getUuid())));
    }

    public Mono<ModelPortfolio> upsertModelPortfolio(ModelPortfolio modelPortfolio) {
        Objects.requireNonNull(modelPortfolio, "ModelPortfolio must not be null");

        return upsertModelPortfolioResponse(modelPortfolio)
            .map(response -> {
                modelPortfolio.uuid(response.getUuid());
                return modelPortfolio;
            })
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.warn("Continuing without portfolio model: externalId={}, name={}, riskLevel={}, status={}",
                    modelPortfolio.getExternalId(), modelPortfolio.getName(), modelPortfolio.getRiskLevel(),
                    ex.getStatusCode());
                return Mono.empty();
            });
    }

    /**
     * Upserts a model portfolio via the Financial Advice API.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Searches for existing model portfolios by {@code external_id}, then by name</li>
     *   <li>If found, patches the existing model portfolio</li>
     *   <li>If not found, creates a new model portfolio</li>
     * </ol>
     *
     * @param modelPortfolio the model portfolio to upsert (must not be null)
     * @return Mono emitting the created or updated model portfolio response
     * @throws NullPointerException if modelPortfolio is null
     */
    private Mono<OASModelPortfolioResponse> upsertModelPortfolioResponse(ModelPortfolio modelPortfolio) {
        Objects.requireNonNull(modelPortfolio, "ModelPortfolio must not be null");

        String modelName = modelPortfolio.getName();
        Integer riskLevel = modelPortfolio.getRiskLevel();

        log.debug("Upserting model portfolio: externalId={}, name={}, riskLevel={}",
            modelPortfolio.getExternalId(), modelName, riskLevel);

        return findExistingModelPortfolio(modelPortfolio)
            .flatMap(existing -> {
                if (existing.expanded() != null && isTargetAssetWeightCorrect(existing.expanded())) {
                    InvestorModelPortfolio pm = existing.expanded();
                    modelPortfolio.setAllocations(modelPortfolioMapper.mapAssetModel(pm.getAllocation()));
                    modelPortfolio.setCashWeight(pm.getCashWeight());
                } else if (existing.expanded() != null) {
                    log.error(
                        "Stored model target asset weight and cash weight are incorrect for uuid={}, name={}, riskLevel={}",
                        existing.uuid(), modelName, riskLevel);
                }
                return patchModelPortfolio(existing.uuid(), modelPortfolio);
            })
            .switchIfEmpty(Mono.defer(() -> createNewModelPortfolio(modelPortfolio)))
            .doOnSuccess(upserted -> log.info(
                "Successfully upserted model portfolio: uuid={}, externalId={}, name={}, riskLevel={}",
                upserted.getUuid(), upserted.getExternalId(), upserted.getName(), upserted.getRiskLevel()));
    }

    private record ExistingModelRef(UUID uuid, InvestorModelPortfolio expanded) {
    }

    private Mono<ExistingModelRef> findExistingModelPortfolio(ModelPortfolio modelPortfolio) {
        if (StringUtils.hasText(modelPortfolio.getExternalId())) {
            return findExistingByExternalId(modelPortfolio.getExternalId())
                .switchIfEmpty(Mono.defer(() -> listExistingModelPortfolios(modelPortfolio.getName(),
                        modelPortfolio.getRiskLevel())
                    .map(inv -> new ExistingModelRef(inv.getUuid(), inv))));
        }
        return listExistingModelPortfolios(modelPortfolio.getName(), modelPortfolio.getRiskLevel())
            .map(inv -> new ExistingModelRef(inv.getUuid(), inv));
    }

    private Mono<ExistingModelRef> findExistingByExternalId(String externalId) {
        // Do not expand allocation.asset here: expanded assets are objects in JSON but the
        // generated OAS list type expects UUID references, which causes DecodingException.
        return financialAdviceApi.listModelPortfolio(
                null, null, null,
                config.getPortfolio().getListModelPageSize(), null, null, null, null, null, null)
            .doOnError(throwable -> log.error("Failed to list model portfolios for externalId={}", externalId,
                throwable))
            .onErrorResume(throwable -> {
                log.warn("Falling back to name-based model lookup after list-by-external-id failed: externalId={}",
                    externalId);
                return Mono.empty();
            })
            .flatMap(page -> {
                List<OASModelPortfolioResponse> results = page != null
                    ? Objects.requireNonNullElse(page.getResults(), List.of())
                    : List.of();
                Optional<OASModelPortfolioResponse> match = results.stream()
                    .filter(model -> externalId.equals(model.getExternalId()))
                    .findFirst();
                if (match.isEmpty()) {
                    log.info("No existing model portfolio found for externalId={}", externalId);
                    return Mono.empty();
                }
                OASModelPortfolioResponse model = match.get();
                log.info("Found existing model portfolio by externalId: externalId={}, uuid={}, name={}",
                    externalId, model.getUuid(), model.getName());
                return Mono.just(new ExistingModelRef(model.getUuid(), null));
            });
    }

    private boolean isTargetAssetWeightCorrect(InvestorModelPortfolio pm) {
        double cashWeight = Optional.ofNullable(pm.getCashWeight()).orElse(0d);
        double assetsWeight = Optional.ofNullable(pm.getAllocation()).orElse(List.of()).stream()
            .mapToDouble(AssetModelPortfolio::getWeight).sum();
        return Math.abs(assetsWeight + cashWeight - 1d) <= WEIGHT_SUM_TOLERANCE;
    }

    /**
     * Lists existing model portfolios by name and risk level.
     *
     * @param name      the model portfolio name to search for
     * @param riskLevel the risk level to filter by
     * @return Mono emitting the first matching model portfolio, or empty if no match found
     */
    private Mono<InvestorModelPortfolio> listExistingModelPortfolios(String name, Integer riskLevel) {
        return financialAdviceApi.listModelPortfolioWithResponseSpec(
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()), null, null,
                config.getPortfolio().getListModelPageSize(), name, null, null, null, null, null)
            .bodyToMono(PaginatedExpandedModelPortfolioList.class)
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

                InvestorModelPortfolio existingModel = models.getResults().getFirst();
                log.info("Found existing model portfolio: uuid={}, name={}, riskLevel={}",
                    existingModel.getUuid(), name, riskLevel);
                return Mono.just(existingModel);
            });
    }

    /**
     * Creates a new model portfolio via the rest service.
     *
     * @param modelPortfolio the model portfolio to create
     * @return Mono emitting the newly created model portfolio
     */
    private Mono<OASModelPortfolioResponse> createNewModelPortfolio(ModelPortfolio modelPortfolio) {
        log.info("Creating new model portfolio: externalId={}, name={}, riskLevel={}",
            modelPortfolio.getExternalId(), modelPortfolio.getName(), modelPortfolio.getRiskLevel());
        return investmentRestModelPortfolioService.createModelPortfolio(modelPortfolio)
            .doOnError(throwable -> logModelPortfolioError("create",
                modelPortfolio.getName(), modelPortfolio.getRiskLevel(), throwable));
    }

    private Mono<OASModelPortfolioResponse> patchModelPortfolio(UUID uuid, ModelPortfolio modelPortfolio) {
        log.debug("Patch model portfolio: uuid={}, object={}", uuid, modelPortfolio);
        return investmentRestModelPortfolioService.patchModelPortfolio(uuid.toString(), modelPortfolio)
            .doOnError(throwable -> logModelPortfolioError("patch",
                modelPortfolio.getName(), modelPortfolio.getRiskLevel(), throwable));
    }

    /**
     * Logs model portfolio operation errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param operation the operation being performed (e.g. "create", "patch")
     * @param name      the name of the model portfolio
     * @param riskLevel the risk level of the model portfolio
     * @param throwable the exception that occurred
     */
    private void logModelPortfolioError(String operation, String name, Integer riskLevel, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to {} model portfolio: name={}, riskLevel={}, status={}, body={}",
                operation, name, riskLevel, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to {} model portfolio: name={}, riskLevel={}", operation,
                name, riskLevel, throwable);
        }
    }

}
