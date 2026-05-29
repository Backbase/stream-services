package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductList;
import com.backbase.investment.api.service.v1.model.PatchedPortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.ProductPortfolio;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestProductPortfolioService;
import com.backbase.stream.investment.service.resttemplate.RestTemplateModelPortfolioMapper;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
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
public class InvestmentPortfolioProductService {

    private final InvestmentProductsApi productsApi;
    private final IngestConfigProperties config;
    private final InvestmentModelPortfolioService modelPortfolioService;
    private final InvestmentRestProductPortfolioService investmentRestProductPortfolioService;
    private final RestTemplateModelPortfolioMapper modelPortfolioMapper =
        Mappers.getMapper(RestTemplateModelPortfolioMapper.class);

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
     * @param investmentArrangements the investment arrangement to associate with the product (must not be null)
     * @return Mono emitting the created or updated portfolio product
     * @throws NullPointerException if investmentArrangement is null
     */
    public Mono<List<PortfolioProduct>> upsertInvestmentProducts(InvestmentData investmentData,
        List<InvestmentArrangement> investmentArrangements) {
        if (investmentArrangements == null) {
            return Mono.error(new NullPointerException("InvestmentArrangement must not be null"));
        }
        return Flux.fromIterable(investmentData.getPortfolioProducts())
            .flatMap(pp -> {
                Mono<ModelPortfolio> modelPortfolio = upsertPortfolioModel(pp);
                return modelPortfolio
                    // todo: improve upsert load
                    .map(mp -> {
                        InvestorModelPortfolio map = modelPortfolioMapper.map(mp);
                        pp.setModelPortfolio(map);
                        return pp;
                    })
                    .switchIfEmpty(Mono.just(pp));
            })
            .collectList()
            .flatMapIterable(this::distinctProducts)
            .flatMap(p -> listExistingPortfolioProducts(p)
                .flatMap(
                    existingProduct -> updateExistingPortfolioProduct(existingProduct, p, investmentData)
                        .onErrorResume(WebClientResponseException.class, ex -> {
                            log.info("Using existing product data due to patch failure: uuid={}",
                                existingProduct.getUuid());
                            return Mono.just(existingProduct);
                        })
                )
                .switchIfEmpty(Mono.defer(() -> createPortfolioProductWithModel(p, investmentData)))
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
                    .sorted(Comparator.nullsLast(Comparator.comparing(PortfolioProduct::getOrder)))
                    .filter(p -> p.getProductType().toString().equalsIgnoreCase(a.getProductTypeExternalId())
                        && Optional.ofNullable(a.getRiskLevel())
                        .map(r -> r.equals(p.getModelPortfolio().getRiskLevel()))
                        .orElse(true))
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

    private Mono<ModelPortfolio> upsertPortfolioModel(ProductPortfolio pp) {
        if (pp.getProductType() == ProductTypeEnum.SELF_TRADING) {
            return Mono.empty();
        }
        InvestorModelPortfolio modelPortfolio = pp.getModelPortfolio();
        if (modelPortfolio == null) {
            return Mono.empty();
        }
        return modelPortfolioService.upsertModelPortfolio(modelPortfolio);
    }

    private Collection<ProductPortfolio> distinctProducts(List<ProductPortfolio> products) {
        return products.stream()
            .collect(Collectors.toMap(
                ProductPortfolio::getName,
                pp -> pp,
                (existing, replacement) -> replacement
            ))
            .values();
    }

    private static List<ModelPortfolio> findModelUuid(InvestmentData investmentData,
        ProductTypeEnum productType) {
        return investmentData.getModelPortfolios().stream()
            .filter(p -> p.getProductTypeEnum() == productType)
            .toList();
    }

    private Mono<PortfolioProduct> listExistingPortfolioProducts(ProductPortfolio portfolioProduct) {
        Integer riskLevel = Optional.ofNullable(portfolioProduct.getModelPortfolio())
            .map(InvestorModelPortfolio::getRiskLevel).orElse(null);

        ProductTypeEnum productType = portfolioProduct.getProductType();

        int pageSize = config.getPortfolio().getListProductPageSize();
        AtomicInteger pageCounter = new AtomicInteger(0);
        return productsApi.listPortfolioProducts(List.of(config.getAllocation().getModelPortfolioAllocationAsset()),
                null, null,
                pageSize, null, null, null, null, null, "-model_portfolio__risk_level",
                List.of(productType.getValue()), null, null)
            .expand(response -> {
                if (response.getNext() == null) {
                    return Mono.empty();
                }
                int nextPage = pageCounter.incrementAndGet();

                return productsApi.listPortfolioProducts(
                    List.of(config.getAllocation().getModelPortfolioAllocationAsset()),
                    null, null,
                    pageSize, null, null, riskLevel, nextPage * pageSize,
                    null, "-model_portfolio__risk_level", List.of(productType.getValue()), null, null);
            })
            .collectList()
            .doOnSuccess(products -> log.debug(
                "List portfolio products query completed: productType={}, found={} results",
                Optional.of(productType),
                products != null ? products.stream().map(PaginatedPortfolioProductList::getCount)
                    .mapToLong(o -> o).sum()
                    : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing portfolio products: productType={}", productType, throwable))
            .flatMap(products -> {
                List<PortfolioProduct> portfolioProducts = Objects.requireNonNullElse(products,
                        List.<PaginatedPortfolioProductList>of()).stream()
                    .map(PaginatedPortfolioProductList::getResults)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .filter(p -> p.getName().equals(portfolioProduct.getName()))
                    .toList();
                if (portfolioProducts.isEmpty()) {
                    log.info("No existing investment product found with productType={}", productType);
                    return Mono.empty();
                }

                int resultCount = portfolioProducts.size();
                if (resultCount > 1) {
                    log.error("Data setup issue: Found {} portfolio products with productType={}, "
                            + "expected exactly 1. Please review product configuration.",
                        resultCount, productType);
                    return Mono.error(new IllegalStateException(
                        String.format("Data setup issue: Found %d portfolio products with productType=%s, "
                                + "expected exactly 1. Please review product configuration.",
                            resultCount, productType)));
                }

                PortfolioProduct existingProduct = portfolioProducts.get(0);
                log.info("Found existing investment product: uuid={}, productType={}",
                    existingProduct.getUuid(), productType);
                return Mono.just(existingProduct);
            });
    }

    /**
     * Updates an existing portfolio product by patching it. Falls back to the original product if the patch operation
     * fails.
     *
     * @param existingProduct  the existing product to update
     * @param portfolioProduct the template product containing desired values
     * @param investmentData   the investment data context used to register the updated product
     * @return Mono emitting the updated product
     */
    private Mono<PortfolioProduct> updateExistingPortfolioProduct(PortfolioProduct existingProduct,
        ProductPortfolio portfolioProduct, InvestmentData investmentData) {
        UUID productUuid = existingProduct.getUuid();

        PatchedPortfolioProductCreateUpdateRequest patch = new PatchedPortfolioProductCreateUpdateRequest()
            .image(null)
            .name(portfolioProduct.getName())
            .description(portfolioProduct.getDescription())
            .badge(portfolioProduct.getBadge())
            .externalId(portfolioProduct.getExternalId())
            .status(portfolioProduct.getStatus())
            .order(portfolioProduct.getOrder())
            .adviceEngine(portfolioProduct.getAdviceEngine())
            .modelPortfolio(Optional.ofNullable(portfolioProduct.getModelPortfolio())
                .map(InvestorModelPortfolio::getUuid)
                .orElse(null))
            .productType(portfolioProduct.getProductType())
            .productCategory(portfolioProduct.getProductCategory())
            .extraData(portfolioProduct.getExtraData());

        log.debug("Attempting to patch existing portfolio product: uuid={}, productType={}",
            productUuid, portfolioProduct.getProductType());

        return productsApi.patchPortfolioProduct(productUuid.toString(),
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()),
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

    // This is next step for implementation
    /*private Mono<PortfolioProduct> updateExistingPortfolioProduct(PortfolioProduct existingProduct,
        ProductPortfolio portfolioProduct, InvestmentData investmentData) {
        UUID productUuid = existingProduct.getUuid();

        log.debug("Attempting to patch existing portfolio product: uuid={}, productType={}",
            productUuid, portfolioProduct.getProductType());

        return investmentRestProductPortfolioService.patchPortfolioProduct(productUuid.toString(),
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()), portfolioProduct)
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
    }*/

    /**
     * Creates a portfolio product with an optional model portfolio UUID.
     *
     * @param portfolioProduct the portfolio product template
     * @param investmentData   the investment data context used to register the created product
     * @return Mono emitting the newly created portfolio product
     */
    private Mono<PortfolioProduct> createPortfolioProductWithModel(ProductPortfolio portfolioProduct,
        InvestmentData investmentData) {

        String productType = portfolioProduct.getProductType().getValue();
        UUID modelPortfolioUuid = Optional.ofNullable(portfolioProduct.getModelPortfolio())
            .map(InvestorModelPortfolio::getUuid).orElse(null);
        log.info("Creating portfolio product: productType={}, modelPortfolioUuid={}",
            productType, modelPortfolioUuid);

        PortfolioProductCreateUpdateRequest request = new PortfolioProductCreateUpdateRequest()
            .image(null)
            .name(portfolioProduct.getName())
            .description(portfolioProduct.getDescription())
            .badge(portfolioProduct.getBadge())
            .externalId(portfolioProduct.getExternalId())
            .status(portfolioProduct.getStatus())
            .order(portfolioProduct.getOrder())
            .adviceEngine(portfolioProduct.getAdviceEngine())
            .modelPortfolio(modelPortfolioUuid)
            .productType(portfolioProduct.getProductType())
            .productCategory(portfolioProduct.getProductCategory())
            .extraData(portfolioProduct.getExtraData());

        return productsApi.createPortfolioProduct(request,
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()), null, null)
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

    // This is next step for implementation
    /*private Mono<PortfolioProduct> createPortfolioProductWithModel(ProductPortfolio portfolioProduct,
        InvestmentData investmentData) {

        String productType = portfolioProduct.getProductType().getValue();
        UUID modelPortfolioUuid = Optional.ofNullable(portfolioProduct.getModelPortfolio())
            .map(InvestorModelPortfolio::getUuid).orElse(null);
        log.info("Creating portfolio product: productType={}, modelPortfolioUuid={}",
            productType, modelPortfolioUuid);

        return investmentRestProductPortfolioService.createPortfolioProduct(portfolioProduct,
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()))
            .retry(2)
            .retryWhen(reactor.util.retry.Retry.fixedDelay(1, java.time.Duration.ofSeconds(1)))
            .doOnSuccess(created -> {
                log.info(
                    "Successfully created portfolio product: uuid={}, productType={}, modelPortfolio={}",
                    created.getUuid(), created.getProductType(), modelPortfolioUuid);
                investmentData.addPortfolioProducts(created);
            })
            .doOnError(throwable -> logPortfolioProductCreationError(productType, throwable));
    }*/

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
