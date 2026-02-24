package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.backbase.investment.api.service.v1.model.Deposit;
import com.backbase.investment.api.service.v1.model.DepositRequest;
import com.backbase.investment.api.service.v1.model.DepositTypeEnum;
import com.backbase.investment.api.service.v1.model.IntegrationPortfolioCreateRequest;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PaginatedDepositList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioTradingAccountList;
import com.backbase.investment.api.service.v1.model.PatchedPortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PatchedPortfolioUpdateRequest;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PortfolioTradingAccount;
import com.backbase.investment.api.service.v1.model.PortfolioTradingAccountRequest;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.investment.api.service.v1.model.Status08fEnum;
import com.backbase.investment.api.service.v1.model.StatusA3dEnum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolioTradingAccount;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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
public class InvestmentPortfolioService {

    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String MODEL_PORTFOLIO_ALLOCATION_ASSET = "model_portfolio.allocation.asset";

    private final InvestmentProductsApi productsApi;
    private final PortfolioApi portfolioApi;
    private final PaymentsApi paymentsApi;
    private final PortfolioTradingAccountsApi portfolioTradingAccountsApi;
    private final InvestmentIngestionConfigurationProperties config;

    public Mono<List<PortfolioList>> upsertPortfolios(List<InvestmentArrangement> investmentArrangements,
        Map<String, List<UUID>> clientsByLeExternalId) {
        return Flux.fromIterable(investmentArrangements)
            .flatMap(arrangement -> {
                log.debug("Upserting investment portfolio for arrangement: externalId={}, name={}, productId={}",
                    arrangement.getExternalId(), arrangement.getName(), arrangement.getInvestmentProductId());

                return upsertInvestmentPortfolios(arrangement, clientsByLeExternalId)
                    .doOnSuccess(portfolio -> log.debug(
                        "Successfully upserted investment portfolio: portfolioUuid={}, externalId={}, name={}",
                        portfolio.getUuid(), portfolio.getExternalId(), portfolio.getName()))
                    .doOnError(throwable -> log.error(
                        "Failed to upsert investment portfolio: arrangementExternalId={}, arrangementName={}",
                        arrangement.getExternalId(), arrangement.getName(), throwable));
            })
            .collectList();
    }

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
    public Mono<List<PortfolioProduct>> upsertInvestmentProducts(InvestmentData investmentData,
        List<InvestmentArrangement> investmentArrangements) {
        Objects.requireNonNull(investmentArrangements, "InvestmentArrangement must not be null");

        return Flux.fromIterable(investmentArrangements)
            .flatMap(investmentArrangement -> {
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
                        log.error(
                            "Data setup issue: Investment portfolio model is not defined for arrangement externalId={}",
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
                return Mono.just(new PortfolioProduct(null, adviceEngine, portfolioModel, productType));
            })
            .collectList()
            .flatMapIterable(this::distinctProducts)
            .flatMap(p -> listExistingPortfolioProducts(p)
                .flatMap(
                    existingProduct -> updateExistingPortfolioProduct(existingProduct, p,
                        investmentData))
                .switchIfEmpty(createPortfolioProductWithModel(p, investmentData))
                .doOnSuccess(product -> log.info(
                    "Successfully upserted portfolio product: engine={}, productType={}, model={}",
                    product.getAdviceEngine(), product.getProductType(),
                    Optional.ofNullable(product.getModelPortfolio())
                        .map(InvestorModelPortfolio::getName).orElse("")))
                .doOnError(throwable -> log.error("Failed to upsert portfolio product: productType={}",
                    p.getProductType(), throwable))
            )
            .collectList()
            .flatMap(products -> {
                investmentArrangements.forEach(a -> products.stream()
                    .filter(p -> p.getProductType().toString().equalsIgnoreCase(a.getProductTypeExternalId()))
                    .findAny().ifPresent(p -> {
                        log.info(
                            "Assign portfolio product to arrangement: engine={}, productType={}, model={}, arrangementName={}",
                            p.getAdviceEngine(), p.getProductType(),
                            Optional.ofNullable(p.getModelPortfolio())
                                .map(InvestorModelPortfolio::getName).orElse(""), a.getName());
                        a.setInvestmentProductId(p.getUuid());
                    }));
                return Mono.just(products);
            })
            .doOnSuccess(products -> {
                log.info(
                    "Successfully upserted investment products {}", products.size());
                log.debug("Successfully upserted investment products {}", products);
            })
            .doOnError(throwable -> log.error(
                "Failed to upsert investment products for arrangements {}", investmentArrangements, throwable));
    }

    private Collection<PortfolioProduct> distinctProducts(List<PortfolioProduct> products) {
        return products.stream()
            .collect(Collectors.toMap(
                pp -> pp.getProductType().getValue() + "_" + Optional.ofNullable(pp.getModelPortfolio())
                    .map(InvestorModelPortfolio::getRiskLevel)
                    .map(Object::toString)
                    .orElse(""),
                pp -> pp,
                (existing, replacement) -> existing
            ))
            .values();
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

    private Mono<PortfolioProduct> listExistingPortfolioProducts(PortfolioProduct portfolioProduct) {
        Integer modelPortfolioRiskLower = null;
        ProductTypeEnum productType = portfolioProduct.getProductType();
        if (ProductTypeEnum.SELF_TRADING != productType) {
            modelPortfolioRiskLower = portfolioProduct.getModelPortfolio().getRiskLevel();
        }
        return listExistingPortfolioProducts(productType, modelPortfolioRiskLower);
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
                .transactedAt(portfolio.getActivated().plusDays(1))
                .completedAt(portfolio.getActivated().plusDays(2))
                .amount(defaultAmount)
                .depositType(DepositTypeEnum.TRANSFER)
                .reason("Initial deposit")
            )
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
     * Upserts portfolio trading accounts derived from the provided investment portfolio accounts.
     *
     * <p>This method constructs {@link PortfolioTradingAccount} instances directly from
     * {@link InvestmentPortfolioTradingAccount} data, resolving each account's portfolio UUID
     * via the portfolio service before upserting.
     *
     * <p>Processing flow:
     * <ol>
     *   <li>Validates input list is not null or empty</li>
     *   <li>For each investment portfolio account:
     *     <ul>
     *       <li>Resolves the portfolio UUID from {@code portfolioExternalId}</li>
     *       <li>Constructs a {@link PortfolioTradingAccount} with all available fields</li>
     *       <li>Creates or updates the trading account via the API</li>
     *     </ul>
     *   </li>
     *   <li>Failed accounts are logged and skipped to prevent batch failures</li>
     * </ol>
     *
     * @param investmentPortfolioTradingAccounts the source accounts containing all required field data
     * @return Mono emitting a list of successfully upserted trading accounts, or an empty list if input is null/empty
     */
    public Mono<List<PortfolioTradingAccount>> upsertPortfolioTradingAccounts(
        List<InvestmentPortfolioTradingAccount> investmentPortfolioTradingAccounts) {

        log.info("Upserting portfolio trading accounts: count={}",
            investmentPortfolioTradingAccounts != null ? investmentPortfolioTradingAccounts.size() : 0);

        if (investmentPortfolioTradingAccounts == null || investmentPortfolioTradingAccounts.isEmpty()) {
            return Mono.just(List.of());
        }

        return Flux.fromIterable(investmentPortfolioTradingAccounts)
            .flatMap(this::upsertSingleTradingAccount)
            .collectList();
    }

    /**
     * Upserts a single portfolio trading account derived from an investment portfolio account.
     *
     * <p>This method:
     * <ol>
     *   <li>Resolves the internal portfolio UUID from the account's {@code portfolioExternalId}</li>
     *   <li>Builds a {@link PortfolioTradingAccountRequest} directly from the source account and resolved UUID</li>
     *   <li>Creates or updates the trading account via the API</li>
     * </ol>
     *
     * <p>Errors are logged and result in an empty Mono to prevent failing the entire batch.
     *
     * @param investmentPortfolioTradingAccount the source account containing all required field data
     * @return Mono emitting the upserted trading account, or empty if processing fails
     */
    private Mono<PortfolioTradingAccount> upsertSingleTradingAccount(
        InvestmentPortfolioTradingAccount investmentPortfolioTradingAccount) {

        String externalAccountId = investmentPortfolioTradingAccount.getAccountExternalId();
        log.debug("Processing trading account: externalAccountId={}", externalAccountId);

        return fetchPortfolioInternalId(investmentPortfolioTradingAccount.getPortfolioExternalId())
            .map(portfolioUuid -> buildTradingAccountRequest(investmentPortfolioTradingAccount, portfolioUuid))
            .flatMap(this::upsertPortfolioTradingAccount)
            .onErrorResume(throwable -> {
                log.warn("Skipping trading account due to error: externalAccountId={}", externalAccountId, throwable);
                return Mono.empty();
            });
    }

    /**
     * Fetches the internal portfolio UUID using the portfolio external ID.
     *
     * <p>Queries the portfolio service to retrieve the internal UUID
     * corresponding to the given external ID.
     *
     * @param portfolioExternalId the external ID of the portfolio
     * @return Mono emitting the resolved portfolio UUID
     * @throws IllegalStateException if the portfolio cannot be found or multiple results are returned
     */
    private Mono<UUID> fetchPortfolioInternalId(String portfolioExternalId) {
        if (portfolioExternalId == null) {
            log.warn("Cannot fetch portfolio internal ID: portfolioExternalId is null");
            return Mono.empty();
        }

        log.debug("Fetching portfolio internal ID: externalId={}", portfolioExternalId);

        return listExistingPortfolios(portfolioExternalId)
            .map(PortfolioList::getUuid)
            .doOnSuccess(uuid -> log.debug("Resolved portfolio UUID={} for externalId={}",
                uuid, portfolioExternalId))
            .doOnError(throwable -> log.error(
                "Failed to fetch portfolio internal ID: externalId={}",
                portfolioExternalId, throwable));
    }

    /**
     * Builds a {@link PortfolioTradingAccountRequest} directly from an {@link InvestmentPortfolioTradingAccount}
     * and a resolved portfolio UUID, skipping the intermediate {@link PortfolioTradingAccount} domain object.
     *
     * <p>Maps the following fields:
     * <ul>
     *   <li>{@code accountExternalId} → {@code externalAccountId}</li>
     *   <li>{@code isDefault} → {@code isDefault}</li>
     *   <li>{@code isInternal} → {@code isInternal}</li>
     *   <li>{@code productTypeExternalId} → {@code accountId}</li>
     *   <li>resolved {@code portfolioUuid} → {@code portfolio}</li>
     * </ul>
     *
     * @param investmentPortfolioTradingAccount the source account containing field data
     * @param portfolioUuid the resolved internal portfolio UUID
     * @return the constructed {@link PortfolioTradingAccountRequest}
     */
    private PortfolioTradingAccountRequest buildTradingAccountRequest(
        InvestmentPortfolioTradingAccount investmentPortfolioTradingAccount, UUID portfolioUuid) {

        return new PortfolioTradingAccountRequest()
            .portfolio(portfolioUuid)
            .accountId(investmentPortfolioTradingAccount.getAccountId())
            .externalAccountId(investmentPortfolioTradingAccount.getAccountExternalId())
            .isDefault(investmentPortfolioTradingAccount.getIsDefault())
            .isInternal(investmentPortfolioTradingAccount.getIsInternal());
    }

    /**
     * Upserts a portfolio trading account using the provided request.
     *
     * <p>Implementation of upsert pattern:
     * <ol>
     *   <li>Searches for an existing trading account by external account ID</li>
     *   <li>If found, patches the existing account with the new values</li>
     *   <li>If not found, creates a new trading account</li>
     * </ol>
     *
     * @param request the trading account request containing all necessary fields
     * @return Mono emitting the created or updated trading account
     */
    public Mono<PortfolioTradingAccount> upsertPortfolioTradingAccount(PortfolioTradingAccountRequest request) {

        return listExistingPortfolioTradingAccounts(request)
            .flatMap(existing -> patchExistingPortfolioTradingAccount(existing, request))
            .switchIfEmpty(createPortfolioTradingAccount(request))
            .doOnSuccess(account -> log.info(
                "Successfully upserted portfolio trading account: uuid={}, externalAccountId={}",
                account.getUuid(), request.getExternalAccountId()))
            .doOnError(throwable -> log.error(
                "Failed to upsert portfolio trading account: externalAccountId={}",
                request.getExternalAccountId(), throwable));
    }

    /**
     * Patches an existing portfolio trading account with updated values.
     *
     * <p>If the patch operation fails (e.g., due to validation errors or conflicts),
     * falls back to returning the existing account to preserve data integrity
     * and prevent batch failures.
     *
     * @param existing the existing trading account to update
     * @param request the request containing updated values
     * @return Mono emitting the updated trading account, or the existing account if patch fails
     */
    private Mono<PortfolioTradingAccount> patchExistingPortfolioTradingAccount(
        PortfolioTradingAccount existing,
        PortfolioTradingAccountRequest request) {

        String uuid = existing.getUuid().toString();
        log.debug("Patching portfolio trading account: uuid={}, externalAccountId={}",
            uuid, request.getExternalAccountId());

        return portfolioTradingAccountsApi.patchPortfolioTradingAccount(uuid, request)
            .doOnSuccess(updated -> log.info(
                "Successfully patched portfolio trading account: uuid={}", updated.getUuid()))
            .doOnError(throwable -> logPortfolioTradingAccountError("PATCH", "uuid", uuid, throwable))
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.info("Using existing portfolio trading account due to patch failure: uuid={}", uuid);
                return Mono.just(existing);
            });
    }

    /**
     * Creates a new portfolio trading account via the API.
     *
     * <p>Called during the upsert flow when no existing trading account is found.
     *
     * @param request the request containing all required trading account details
     * @return Mono emitting the newly created trading account
     */
    public Mono<PortfolioTradingAccount> createPortfolioTradingAccount(PortfolioTradingAccountRequest request) {

        return portfolioTradingAccountsApi.createPortfolioTradingAccount(request)
            .doOnSuccess(account -> log.info(
                "Created portfolio trading account: uuid={}, externalAccountId={}",
                account.getUuid(), request.getExternalAccountId()))
            .doOnError(throwable -> logPortfolioTradingAccountError(
                "CREATE", "externalAccountId", request.getExternalAccountId(), throwable));
    }

    /**
     * Lists existing portfolio trading accounts matching the external account ID in the request.
     *
     * <p>Validates the result to ensure data consistency:
     * <ul>
     *   <li>Returns empty if no matching account is found</li>
     *   <li>Returns the single matching account if exactly one result is found</li>
     *   <li>Returns an error if multiple accounts are found (indicates a data setup issue)</li>
     * </ul>
     *
     * @param request the request containing the external account ID to search for
     * @return Mono emitting the matching trading account, or empty if not found
     * @throws IllegalStateException if more than one trading account is found with the same external account ID
     */
    private Mono<PortfolioTradingAccount> listExistingPortfolioTradingAccounts(
        PortfolioTradingAccountRequest request) {

        String externalAccountId = request.getExternalAccountId();

        return portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                1, null, null, externalAccountId, null, null, null)
            .doOnSuccess(accounts -> log.debug(
                "List portfolio trading accounts query completed: externalAccountId={}, found={} results",
                externalAccountId, accounts != null ? accounts.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing portfolio trading accounts: externalAccountId={}",
                externalAccountId, throwable))
            .flatMap(accounts -> validateAndExtractPortfolioTradingAccount(accounts, externalAccountId));
    }

    /**
     * Validates and extracts a single trading account from paginated search results.
     *
     * <p>Enforces data consistency:
     * <ul>
     *   <li>Returns empty for no results — expected for new accounts</li>
     *   <li>Returns the account for exactly one result</li>
     *   <li>Returns an error for multiple results — indicates a data setup issue</li>
     * </ul>
     *
     * @param accounts the paginated list of trading accounts returned by the API
     * @param externalAccountId the external account ID used in the search, for logging purposes
     * @return Mono emitting the single matching trading account, or empty if no results found
     * @throws IllegalStateException if multiple trading accounts are found with the same external account ID
     */
    private Mono<PortfolioTradingAccount> validateAndExtractPortfolioTradingAccount(
        PaginatedPortfolioTradingAccountList accounts,
        String externalAccountId) {

        if (accounts == null || CollectionUtils.isEmpty(accounts.getResults())) {
            log.info("No existing portfolio trading account found: externalAccountId={}", externalAccountId);
            return Mono.empty();
        }

        int resultCount = accounts.getResults().size();
        if (resultCount > 1) {
            log.error("Data setup issue: Found {} portfolio trading accounts with externalAccountId={}, "
                    + "expected at most 1. Please review trading account configuration.",
                resultCount, externalAccountId);
            return Mono.error(new IllegalStateException(
                String.format("Data setup issue: Found %d portfolio trading accounts with externalAccountId=%s, "
                        + "expected at most 1. Please review trading account configuration.",
                    resultCount, externalAccountId)));
        }

        PortfolioTradingAccount existingAccount = accounts.getResults().getFirst();
        log.info("Found existing portfolio trading account: uuid={}, externalAccountId={}",
            existingAccount.getUuid(), externalAccountId);
        return Mono.just(existingAccount);
    }

    /**
     * Logs errors occurring during portfolio trading account operations.
     *
     * <p>Provides enhanced error context for {@link WebClientResponseException},
     * including HTTP status code and response body. For other exceptions, logs
     * basic error information.
     *
     * @param operation  a short description of the operation that failed (e.g., "PATCH", "CREATE")
     * @param idLabel    the label for the identifier (e.g., "uuid", "externalAccountId")
     * @param idValue    the value of the identifier
     * @param throwable  the exception that occurred
     */
    private void logPortfolioTradingAccountError(String operation, String idLabel, String idValue, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.warn("Portfolio trading account {} failed: {}={}, status={}, body={}",
                operation, idLabel, idValue, ex.getStatusCode(), ex.getResponseBodyAsString());
        } else {
            log.error("Portfolio trading account {} failed: {}={}", operation, idLabel, idValue, throwable);
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
