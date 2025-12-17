package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.Deposit;
import com.backbase.investment.api.service.v1.model.DepositRequest;
import com.backbase.investment.api.service.v1.model.DepositTypeEnum;
import com.backbase.investment.api.service.v1.model.IntegrationPortfolioCreateRequest;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PaginatedDepositList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductList;
import com.backbase.investment.api.service.v1.model.PatchedPortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PatchedPortfolioUpdateRequest;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.investment.api.service.v1.model.Status08fEnum;
import com.backbase.investment.api.service.v1.model.StatusA3dEnum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
public class InvestmentPortfolioService {

    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String MODEL_PORTFOLIO_ALLOCATION_ASSET = "model_portfolio.allocation.asset";

    private final InvestmentProductsApi productsApi;
    private final PortfolioApi portfolioApi;
    private final PaymentsApi paymentsApi;
    private final InvestmentIngestionConfigurationProperties config;

    /**
     * Creates or updates an investment product (portfolio product) for the given arrangement.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Creates a self-trading portfolio product template</li>
     *   <li>Searches for existing products of the same type</li>
     *   <li>If found, patches the existing product</li>
     *   <li>If not found, creates a new product</li>
     *   <li>Updates the arrangement with the product UUID</li>
     * </ol>
     *
     * @param investmentArrangement the investment arrangement to associate with the product (must not be null)
     * @return Mono emitting the created or updated portfolio product
     * @throws NullPointerException if investmentArrangement is null
     */
    public Mono<PortfolioProduct> upsertInvestmentProducts(InvestmentData investmentData,
        InvestmentArrangement investmentArrangement) {
        Objects.requireNonNull(investmentArrangement, "InvestmentArrangement must not be null");

        String productTypeExternalId = investmentArrangement.getProductTypeExternalId();

        ProductTypeEnum productType = Arrays.stream(ProductTypeEnum.values())
            .filter(pt -> productTypeExternalId.equalsIgnoreCase(pt.getValue()))
            .findFirst().orElse(null);
        if (productType == null) {
            log.error("Data setup issue: Investment does not support type {}", productTypeExternalId);
            return Mono.error(new IllegalStateException(
                String.format("Data setup issue: Investment does not support type=%s",
                    productTypeExternalId)));
        }
        InvestorModelPortfolio portfolioModel = null;
        String adviceEngine = null;
        if (productType != ProductTypeEnum.SELF_TRADING) {
            adviceEngine = "model_portfolio";
            String externalId = investmentArrangement.getExternalId();
            List<ModelPortfolio> modelUuid = findModelUuid(investmentData, externalId, productType);
            if (modelUuid.isEmpty()) {
                log.error("Data setup issue: Investment portfolio model is not defined for arrangement externalId={}",
                    externalId);
                return Mono.error(new IllegalStateException(
                    String.format(
                        "Data setup issue: Investment does have portfolio model for arrangement externalId=%s",
                        externalId)));
            }
            ModelPortfolio modelPortfolio = modelUuid.get(0);
            portfolioModel = new InvestorModelPortfolio(modelPortfolio.getUuid(), null, null,
                modelPortfolio.getRiskLevel(), null, null);
        }
        PortfolioProduct portfolioProduct = new PortfolioProduct(null, adviceEngine, portfolioModel, productType);

        log.info("Upserting investment product: productType={}, arrangementName={}",
            portfolioProduct.getProductType(), investmentArrangement.getName());

        return listExistingPortfolioProducts(portfolioProduct, investmentData)
            .flatMap(
                existingProduct -> updateExistingPortfolioProduct(existingProduct, portfolioProduct, investmentData))
            .switchIfEmpty(createPortfolioProductWithModel(portfolioProduct, investmentData))
            .doOnSuccess(product -> {
                log.info(
                    "Successfully upserted portfolio product: engine={}, productType={}, model={}, arrangementName={}",
                    product.getAdviceEngine(), product.getProductType(),
                    Optional.ofNullable(product.getModelPortfolio())
                        .map(InvestorModelPortfolio::getName).orElse(""), investmentArrangement.getName());
                investmentArrangement.setInvestmentProductId(product.getUuid());
            })
            .doOnError(throwable -> log.error(
                "Failed to upsert portfolio product: productType={}, arrangementName={}",
                productType, investmentArrangement.getName(), throwable));
    }

    private static List<ModelPortfolio> findModelUuid(InvestmentData investmentData, String externalId,
        ProductTypeEnum productType) {
//        Map<String, List<UUID>> modelsByArrangementExternalId = investmentData.getModelsByArrangementExternalId();
//        Optional<List<UUID>> modelUuid = modelsByArrangementExternalId.keySet().stream()
//            .filter(a -> a.endsWith(externalId)).findAny()
//            .map(modelsByArrangementExternalId::get);
        return investmentData.getModelPortfolios().stream()
            .filter(p -> p.getProductTypeEnum() == productType)
            .toList();
    }

    /**
     * Creates or updates an investment portfolio for the given arrangement.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Searches for existing portfolios by arrangement external ID</li>
     *   <li>If found, returns the first matching portfolio</li>
     *   <li>If not found, creates a new portfolio with associated clients</li>
     *   <li>Associates clients from all related legal entities</li>
     * </ol>
     *
     * <p>The method resolves client UUIDs by mapping legal entity external IDs
     * from the arrangement to client UUIDs via the provided lookup map.
     *
     * @param investmentArrangement the investment arrangement containing portfolio details (must not be null)
     * @param clientsByLeExternalId map of legal entity external ID to client UUIDs for associations
     * @return Mono emitting the created or existing portfolio
     * @throws NullPointerException if investmentArrangement is null
     */
    public Mono<PortfolioList> upsertInvestmentPortfolios(InvestmentArrangement investmentArrangement,
        Map<String, List<UUID>> clientsByLeExternalId) {
        Objects.requireNonNull(investmentArrangement, "InvestmentArrangement must not be null");

        String externalId = investmentArrangement.getExternalId();
        String arrangementName = investmentArrangement.getName();

        log.info("Upserting investment portfolio: externalId={}, name={}", externalId, arrangementName);

        return listExistingPortfolios(externalId)
            .flatMap(p -> patchPortfolio(p, investmentArrangement, clientsByLeExternalId))
            .switchIfEmpty(createNewPortfolio(investmentArrangement, clientsByLeExternalId))
            .doOnSuccess(portfolio -> log.info(
                "Successfully upserted investment portfolio: externalId={}, name={}, portfolioUuid={}",
                externalId, arrangementName,
                portfolio != null ? portfolio.getUuid() : "N/A"))
            .doOnError(throwable -> log.error(
                "Failed to upsert investment portfolio: externalId={}, name={}",
                externalId, arrangementName, throwable));
    }

    private Mono<PortfolioList> patchPortfolio(
        PortfolioList existingProduct, InvestmentArrangement investmentArrangement,
        Map<String, List<UUID>> clientsByLeExternalId) {

        String uuid = existingProduct.getUuid().toString();
        List<UUID> associatedClients = getClients(investmentArrangement, clientsByLeExternalId);

        PatchedPortfolioUpdateRequest patchedPortfolioUpdateRequest = new PatchedPortfolioUpdateRequest()
            .product(investmentArrangement.getInvestmentProductId())
            .externalId(investmentArrangement.getExternalId())
            .name(investmentArrangement.getName())
            .clients(associatedClients)
            .status(StatusA3dEnum.ACTIVE)
            .activated(OffsetDateTime.now().minusMonths(config.getPortfolioActivationPastMonths()));

        log.debug("Attempting to patch existing portfolio: uuid={}, externalId={}",
            uuid, investmentArrangement.getExternalId());

        return portfolioApi.patchPortfolio(uuid, null, null, null, patchedPortfolioUpdateRequest)
            .doOnSuccess(updated -> {
                log.info("Successfully patched existing investment portfolio: uuid={}", updated.getUuid());
                investmentArrangement.setInvestmentProductId(updated.getUuid());
            })
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.warn(
                        "PATCH portfolio failed (falling back to existing): uuid={}, status={}, body={}",
                        uuid, ex.getStatusCode(), ex.getResponseBodyAsString());
                } else {
                    log.warn("PATCH portfolio failed (falling back to existing): uuid={}",
                        uuid, throwable);
                }
            })
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.info("Using existing portfolio data due to patch failure: uuid={}", uuid);
                investmentArrangement.setInvestmentProductId(existingProduct.getUuid());
                return Mono.just(existingProduct);
            });
    }

    /**
     * Lists existing portfolios matching the provided external ID.
     *
     * @param externalId the external ID to search for
     * @return Mono emitting the first matching portfolio, or empty if no match found
     */
    private Mono<PortfolioList> listExistingPortfolios(String externalId) {
        return portfolioApi.listPortfolios(null, null, null,
                null, externalId, null, null, 1,
                null, null, null, null)
            .doOnSuccess(plist -> log.debug(
                "List portfolios query completed: externalId={}, found={} results",
                externalId,
                // only one
                plist != null ? plist.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing portfolios: externalId={}", externalId, throwable))
            .flatMap(plist -> {
                if (plist == null || CollectionUtils.isEmpty(plist.getResults())) {
                    log.info("No existing investment portfolio found with externalId={}", externalId);
                    return Mono.empty();
                }
                int resultCount = plist.getResults().size();
                if (resultCount > 1) {
                    log.error("Data setup issue: Found {} portfolios with externalId={}, "
                            + "expected exactly 1. Please review portfolios configuration.",
                        resultCount, externalId);
                    return Mono.error(new IllegalStateException(
                        String.format("Data setup issue: Found %d portfolios with externalId=%s, "
                                + "expected exactly 1. Please review portfolios configuration.",
                            resultCount, externalId)));
                }

                PortfolioList existingPortfolio = plist.getResults().get(0);
                log.info("Found existing investment portfolio: uuid={}, externalId={}",
                    existingPortfolio.getUuid(), externalId);
                return Mono.just(existingPortfolio);
            });
    }

    /**
     * Creates a new investment portfolio with associated clients.
     *
     * @param investmentArrangement the arrangement containing portfolio details
     * @param clientsByLeExternalId map to resolve client UUIDs from legal entity external IDs
     * @return Mono emitting the newly created portfolio
     */
    private Mono<PortfolioList> createNewPortfolio(InvestmentArrangement investmentArrangement,
        Map<String, List<UUID>> clientsByLeExternalId) {
        List<UUID> associatedClients = getClients(investmentArrangement, clientsByLeExternalId);

        log.info("Creating new investment portfolio: externalId={}, name={}, clientCount={}",
            investmentArrangement.getExternalId(), investmentArrangement.getName(), associatedClients.size());

        IntegrationPortfolioCreateRequest request = new IntegrationPortfolioCreateRequest()
            .product(investmentArrangement.getInvestmentProductId())
            .arrangementId(investmentArrangement.getInternalId())
            .externalId(investmentArrangement.getExternalId())
            .name(investmentArrangement.getName())
            .clients(associatedClients)
            .currency(Optional.ofNullable(investmentArrangement.getCurrency()).orElse(DEFAULT_CURRENCY))
            .status(StatusA3dEnum.ACTIVE)
            .activated(OffsetDateTime.now().minusMonths(config.getPortfolioActivationPastMonths()));

        return portfolioApi.createPortfolio(request, null, null, null)
            .doOnSuccess(created -> log.info(
                "Successfully created investment portfolio: uuid={}, externalId={}, name={}, clients={}",
                created.getUuid(), created.getExternalId(), created.getName(), associatedClients.size()))
            .doOnError(throwable -> logPortfolioCreationError(
                investmentArrangement.getExternalId(), investmentArrangement.getName(), throwable));
    }

    /**
     * Resolves client UUIDs from legal entity external IDs.
     *
     * <p>This method maps the legal entity external IDs from the arrangement to client UUIDs
     * using the provided lookup map. It filters out null values and ensures distinct results.
     *
     * @param investmentArrangement the arrangement containing legal entity external IDs
     * @param clientsByLeExternalId map of legal entity external ID to client UUIDs
     * @return distinct list of client UUIDs associated with the arrangement's legal entities
     */
    private static List<UUID> getClients(InvestmentArrangement investmentArrangement,
        Map<String, List<UUID>> clientsByLeExternalId) {
        return investmentArrangement.getLegalEntityExternalIds().stream()
            .map(clientsByLeExternalId::get)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .distinct()
            .toList();
    }

    private Mono<PortfolioProduct> listExistingPortfolioProducts(PortfolioProduct portfolioProduct,
        InvestmentData investmentData) {
        Integer modelPortfolioRiskLower = null;
        ProductTypeEnum productType = portfolioProduct.getProductType();
        if (ProductTypeEnum.SELF_TRADING != productType) {
            modelPortfolioRiskLower = portfolioProduct.getModelPortfolio().getRiskLevel();
        }
        return Mono.justOrEmpty(investmentData.findPortfolioProduct(productType, modelPortfolioRiskLower))
            .switchIfEmpty(listExistingPortfolioProducts(productType, modelPortfolioRiskLower));
    }

    private Mono<PortfolioProduct> listExistingPortfolioProducts(ProductTypeEnum productType, Integer riskLevel) {
        return productsApi.listPortfolioProducts(List.of(MODEL_PORTFOLIO_ALLOCATION_ASSET), null, null,
                1, null, null, riskLevel, null, null, "-model_portfolio__risk_level",
                List.of(productType.getValue()))
            .doOnSuccess(products -> log.debug(
                "List portfolio products query completed: productType={}, found={} results",
                productType, products != null ? products.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing portfolio products: productType={}", productType, throwable))
            .flatMap(products -> {
                if (Optional.ofNullable(products).map(PaginatedPortfolioProductList::getResults).map(List::isEmpty)
                    .orElse(false)) {
                    log.info("No existing investment product found with productType={}", productType);
                    return Mono.empty();
                }

                int resultCount = products.getResults().size();
                if (resultCount > 1) {
                    log.error("Data setup issue: Found {} portfolio products with productType={}, "
                            + "expected exactly 1. Please review product configuration.",
                        resultCount, productType);
                    return Mono.error(new IllegalStateException(
                        String.format("Data setup issue: Found %d portfolio products with productType=%s, "
                                + "expected exactly 1. Please review product configuration.",
                            resultCount, productType)));
                }

                PortfolioProduct existingProduct = products.getResults().get(0);
                log.info("Found existing investment product: uuid={}, productType={}",
                    existingProduct.getUuid(), productType);
                return Mono.just(existingProduct);
            });
    }

    /**
     * Updates an existing portfolio product by patching it. Falls back to the original product if the patch operation
     * fails.
     *
     * @param investmentArrangement the arrangement to update with product UUID
     * @param existingProduct       the existing product to update
     * @param portfolioProduct      the template product containing desired values
     * @param investmentData
     * @return Mono emitting the updated product
     */
    private Mono<PortfolioProduct> updateExistingPortfolioProduct(PortfolioProduct existingProduct,
        PortfolioProduct portfolioProduct, InvestmentData investmentData) {
        UUID productUuid = existingProduct.getUuid();

        PatchedPortfolioProductCreateUpdateRequest patch = new PatchedPortfolioProductCreateUpdateRequest()
            .modelPortfolio(Optional.ofNullable(portfolioProduct.getModelPortfolio())
                .map(InvestorModelPortfolio::getUuid)
                .orElse(null))
            .productType(portfolioProduct.getProductType())
            .adviceEngine(portfolioProduct.getAdviceEngine())
            .extraData(portfolioProduct.getExtraData());

        log.debug("Attempting to patch existing portfolio product: uuid={}, productType={}",
            productUuid, portfolioProduct.getProductType());

        return productsApi.patchPortfolioProduct(productUuid.toString(), List.of(MODEL_PORTFOLIO_ALLOCATION_ASSET),
                null, null, patch)
            .doOnSuccess(updated -> {
                log.info("Successfully patched existing investment product: uuid={}", updated.getUuid());
                investmentData.addPortfolioProducts(updated);
            })
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.warn(
                        "PATCH portfolio product failed (falling back to existing): uuid={}, status={}, body={}",
                        productUuid, ex.getStatusCode(), ex.getResponseBodyAsString());
                } else {
                    log.warn("PATCH portfolio product failed (falling back to existing): uuid={}",
                        productUuid, throwable);
                }
            })
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.info("Using existing product data due to patch failure: uuid={}", productUuid);
                return Mono.just(existingProduct);
            });
    }

    /**
     * Creates a portfolio product with an optional model portfolio UUID.
     *
     * @param investmentArrangement the arrangement to update with product UUID
     * @param portfolioProduct      the portfolio product template
     * @param investmentData
     * @return Mono emitting the newly created portfolio product
     */
    private Mono<PortfolioProduct> createPortfolioProductWithModel(PortfolioProduct portfolioProduct,
        InvestmentData investmentData) {

        String productType = portfolioProduct.getProductType().getValue();
        UUID modelPortfolioUuid = Optional.ofNullable(portfolioProduct.getModelPortfolio())
            .map(InvestorModelPortfolio::getUuid).orElse(null);
        log.info("Creating portfolio product: productType={}, modelPortfolioUuid={}",
            productType, modelPortfolioUuid);

        PortfolioProductCreateUpdateRequest request = new PortfolioProductCreateUpdateRequest()
            .adviceEngine(portfolioProduct.getAdviceEngine())
            .modelPortfolio(modelPortfolioUuid)
            .productType(portfolioProduct.getProductType());

        return productsApi.createPortfolioProduct(request, List.of(MODEL_PORTFOLIO_ALLOCATION_ASSET), null, null)
            .retry(2)
            .retryWhen(reactor.util.retry.Retry.fixedDelay(1, java.time.Duration.ofSeconds(1)))
            .doOnSuccess(created -> {
                log.info(
                    "Successfully created portfolio product: uuid={}, productType={}, modelPortfolio={}",
                    created.getUuid(), created.getProductType(), modelPortfolioUuid);
                investmentData.addPortfolioProducts(created);
            })
            .doOnError(throwable -> logPortfolioProductCreationError(productType, throwable));
    }

    public Mono<Deposit> upsertDeposits(PortfolioList portfolio) {
        double defaultAmount = 10_000d;
        return paymentsApi.listDeposits(null, null, null, null, null,
                null, portfolio.getUuid(), null, null, null)
            .filter(Objects::nonNull)
            .map(PaginatedDepositList::getResults)
            .filter(Objects::nonNull)
            .filter(Predicate.not(CollectionUtils::isEmpty))
            .flatMap(deposits -> {
                Double deposit = deposits.stream().map(Deposit::getAmount).reduce(0d, Double::sum);
                double additionalDeposit = defaultAmount - deposit;
                if (additionalDeposit > 0) {
                    return createDeposit(portfolio, additionalDeposit);
                }
                return Mono.just(deposits.getLast());
            })
            .switchIfEmpty(createDeposit(portfolio, defaultAmount))
            .onErrorResume(ex -> Mono.just(new Deposit()
                    .portfolio(portfolio.getUuid())
                    .amount(defaultAmount)
                    .transactedAt(portfolio.getActivated().plusDays(10))
                    .completedAt(portfolio.getActivated().plusDays(2))
                )
            );
    }

    @Nonnull
    private Mono<Deposit> createDeposit(PortfolioList portfolio, double defaultAmount) {
        return paymentsApi.createDeposit(new DepositRequest()
                .portfolio(portfolio.getUuid())
                .provider("mock")
                .reason(UUID.randomUUID().toString())
                .status(Status08fEnum.COMPLETED)
                .transactedAt(portfolio.getActivated().plusDays(10))
                .completedAt(portfolio.getActivated().plusDays(2))
                .amount(defaultAmount)
                .depositType(DepositTypeEnum.TRANSFER))
            .doOnSuccess(deposit -> log.info("Created deposit {} for portfolio {}",
                deposit.getUuid(), portfolio.getUuid()))
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.warn(
                        "Portfolio deposit create failed: uuid={}, status={}, body={}",
                        portfolio.getUuid(), ex.getStatusCode(), ex.getResponseBodyAsString());
                } else {
                    log.warn("Portfolio deposit create failed: uuid={}",
                        portfolio.getUuid(), throwable);
                }
            });
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
