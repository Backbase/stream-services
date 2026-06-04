package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.ProductPortfolio;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestProductPortfolioService;
import com.backbase.stream.investment.service.resttemplate.RestTemplateModelPortfolioMapper;
import java.time.Duration;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Service wrapper around {@link InvestmentProductsApi} and {@link InvestmentRestProductPortfolioService} providing
 * guarded create/patch operations with logging, minimal idempotency helpers and consistent error handling.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Investment portfolio product creation and updates</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying API clients are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentPortfolioProductService {

    private static final Comparator<PortfolioProduct> BY_ORDER =
        Comparator.nullsLast(Comparator.comparing(PortfolioProduct::getOrder));

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
     * @param investmentData         the investment data context containing portfolio product templates
     * @param investmentArrangements the investment arrangements to associate with products (must not be null)
     * @return Mono emitting the created or updated portfolio products
     * @throws NullPointerException if investmentArrangements is null
     */
    public Mono<List<PortfolioProduct>> upsertInvestmentProducts(InvestmentData investmentData,
        List<InvestmentArrangement> investmentArrangements) {
        if (investmentArrangements == null) {
            return Mono.error(new NullPointerException("investmentArrangements must not be null"));
        }
        return enrichPortfolioProductsWithModels(investmentData)
            .flatMapIterable(this::distinctProducts)
            .flatMap(p -> listExistingPortfolioProducts(p)
                .flatMap(existingProduct -> updateExistingPortfolioProduct(existingProduct, p, investmentData))
                .switchIfEmpty(Mono.defer(() -> createPortfolioProductWithModel(p, investmentData)))
                .doOnSuccess(product -> log.info(
                    "Successfully upserted portfolio product: uuid={}, name={}, engine={}, productType={}, model={}",
                    product.getUuid(), product.getName(), product.getAdviceEngine(), product.getProductType(),
                    Optional.ofNullable(product.getModelPortfolio())
                        .map(InvestorModelPortfolio::getName).orElse("")))
                .doOnError(throwable -> log.error("Failed to upsert portfolio product: name={}, productType={}",
                    p.getName(), p.getProductType(), throwable))
            )
            .collectList()
            .map(products -> {
                assignProductsToArrangements(products, investmentArrangements);
                return products;
            })
            .doOnSuccess(products -> {
                log.info("Successfully upserted portfolio products: count={}", products.size());
                log.debug("Successfully upserted portfolio products: products={}", products);
            })
            .doOnError(throwable -> log.error(
                "Failed to upsert portfolio products: arrangementCount={}", investmentArrangements.size(), throwable));
    }

    private Mono<List<ProductPortfolio>> enrichPortfolioProductsWithModels(InvestmentData investmentData) {
        List<ProductPortfolio> portfolioProducts = Objects.requireNonNullElse(
            investmentData.getPortfolioProducts(), List.of());
        return Flux.fromStream(portfolioProducts.stream())
            .flatMap(pp -> {
                Mono<ModelPortfolio> modelPortfolio = upsertPortfolioModel(pp);
                return modelPortfolio
                    .map(mp -> {
                        InvestorModelPortfolio mappedModelPortfolio = modelPortfolioMapper.map(mp);
                        pp.setModelPortfolio(mappedModelPortfolio);
                        return pp;
                    })
                    .switchIfEmpty(Mono.just(pp));
            })
            .collectList();
    }

    private Mono<ModelPortfolio> upsertPortfolioModel(ProductPortfolio pp) {
        InvestorModelPortfolio modelPortfolio = pp.getModelPortfolio();
        if (modelPortfolio == null) {
            return Mono.empty();
        }
        return modelPortfolioService.upsertModelPortfolio(modelPortfolio);
    }

    private void assignProductsToArrangements(List<PortfolioProduct> products,
        List<InvestmentArrangement> arrangements) {
        arrangements.forEach(arrangement -> findMatchingProduct(products, arrangement)
            .ifPresent(product -> {
                log.info(
                    "Assigned portfolio product to arrangement: productUuid={}, name={}, engine={}, "
                        + "productType={}, model={}, arrangementName={}",
                    product.getUuid(), product.getName(), product.getAdviceEngine(), product.getProductType(),
                    Optional.ofNullable(product.getModelPortfolio())
                        .map(InvestorModelPortfolio::getName).orElse(""),
                    arrangement.getName());
                arrangement.setInvestmentProductId(product.getUuid());
            }));
    }

    private Optional<PortfolioProduct> findMatchingProduct(List<PortfolioProduct> products,
        InvestmentArrangement arrangement) {
        List<PortfolioProduct> typeProducts = products.stream()
            .filter(product -> product.getProductType().toString()
                .equalsIgnoreCase(arrangement.getProductTypeExternalId()))
            .sorted(BY_ORDER)
            .toList();
        if (typeProducts.isEmpty()) {
            log.warn(
                "No portfolio product matched arrangement product type: arrangementName={}, productType={}, "
                    + "productPortfolioName={}, upsertedProductCount={}",
                arrangement.getName(), arrangement.getProductTypeExternalId(),
                arrangement.getProductPortfolioName(), products.size());
            return Optional.empty();
        }
        if (StringUtils.hasText(arrangement.getProductPortfolioName())) {
            return typeProducts.stream()
                .filter(product -> arrangement.getProductPortfolioName().equals(product.getName()))
                .min(BY_ORDER)
                .or(() -> typeProducts.stream().min(BY_ORDER));
        }
        return typeProducts.stream().min(BY_ORDER);
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

    private Mono<PortfolioProduct> listExistingPortfolioProducts(ProductPortfolio portfolioProduct) {
        Integer riskLevel = Optional.ofNullable(portfolioProduct.getModelPortfolio())
            .map(InvestorModelPortfolio::getRiskLevel).orElse(null);

        ProductTypeEnum productType = portfolioProduct.getProductType();
        String productCategory = portfolioProduct.getProductCategory();

        int pageSize = config.getPortfolio().getListProductPageSize();
        AtomicInteger pageCounter = new AtomicInteger(0);
        return productsApi.listPortfolioProducts(List.of(config.getAllocation().getModelPortfolioAllocationAsset()),
                null, null, null, pageSize, null, null, null, null, null, "-model_portfolio__risk_level",
                List.of(productType.getValue()), null, null)
            .expand(response -> {
                if (response.getNext() == null) {
                    return Mono.empty();
                }
                int nextPage = pageCounter.incrementAndGet();

                return productsApi.listPortfolioProducts(
                    List.of(config.getAllocation().getModelPortfolioAllocationAsset()),
                    null, null, null, pageSize, null, riskLevel,
                    null, nextPage * pageSize,
                    null, "-model_portfolio__risk_level", List.of(productType.getValue()), List.of(productCategory),
                    null);
            })
            .collectList()
            .doOnSuccess(products -> log.debug(
                "List portfolio products query completed: name={}, productType={}, found={} results",
                portfolioProduct.getName(), productType,
                products != null ? products.stream().map(PaginatedPortfolioProductList::getCount)
                    .mapToLong(o -> o).sum()
                    : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing portfolio products: name={}, productType={}",
                portfolioProduct.getName(), productType, throwable))
            .flatMap(products -> {
                List<PortfolioProduct> portfolioProducts = Objects.requireNonNullElse(products,
                        List.<PaginatedPortfolioProductList>of()).stream()
                    .map(PaginatedPortfolioProductList::getResults)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .filter(p -> p.getName().equals(portfolioProduct.getName()) || p.getName()
                        .equals(Optional.ofNullable(portfolioProduct.getProductType()).map(ProductTypeEnum::getValue)
                            .orElse(null)))
                    .toList();
                if (portfolioProducts.isEmpty()) {
                    log.info("No existing portfolio product found: name={}, productType={}",
                        portfolioProduct.getName(), productType);
                    return Mono.empty();
                }
                int resultCount = portfolioProducts.size();
                if (resultCount > 1) {
                    // TODO: verify duplicate portfolio product selection strategy (getLast vs fail)
                    log.warn("Found {} portfolio products matching name={}, productType={}, using most recent one",
                        resultCount, portfolioProduct.getName(), productType);
                }

                PortfolioProduct existingProduct = portfolioProducts.getLast();
                log.info("Found existing portfolio product: uuid={}, name={}, productType={}",
                    existingProduct.getUuid(), existingProduct.getName(), productType);
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

        log.debug("Patching existing portfolio product: uuid={}, name={}, productType={}",
            productUuid, portfolioProduct.getName(), portfolioProduct.getProductType());

        return investmentRestProductPortfolioService.updatePortfolioProduct(productUuid.toString(),
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()),
                portfolioProduct)
            .doOnSuccess(updated -> {
                log.debug("Successfully patched portfolio product: uuid={}, name={}, productType={}",
                    updated.getUuid(), updated.getName(), updated.getProductType());
                investmentData.addPortfolioProducts(updated);
            })
            .doOnError(throwable -> logPortfolioProductPatchError(
                productUuid, portfolioProduct.getName(), portfolioProduct.getProductType(), throwable))
            .onErrorResume(HttpClientErrorException.class, ex -> Mono.just(existingProduct));
    }

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
        log.info("Creating portfolio product: name={}, productType={}, modelPortfolioUuid={}",
            portfolioProduct.getName(), productType, modelPortfolioUuid);

        return investmentRestProductPortfolioService.createPortfolioProduct(portfolioProduct,
                List.of(config.getAllocation().getModelPortfolioAllocationAsset()))
            .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
            .doOnSuccess(created -> {
                log.debug(
                    "Successfully created portfolio product: uuid={}, name={}, productType={}, modelPortfolioUuid={}",
                    created.getUuid(), created.getName(), created.getProductType(), modelPortfolioUuid);
                investmentData.addPortfolioProducts(created);
            })
            .doOnError(throwable -> logPortfolioProductCreationError(
                portfolioProduct.getName(), productType, throwable));
    }

    /**
     * Logs portfolio product patch errors with detailed information about the failure.
     *
     * @param productUuid the UUID of the portfolio product being patched
     * @param productName the name of the portfolio product being patched
     * @param productType the product type being patched
     * @param throwable   the exception that occurred during patching
     */
    private void logPortfolioProductPatchError(UUID productUuid, String productName, ProductTypeEnum productType,
        Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.warn(
                "PATCH portfolio product failed, falling back to existing product: uuid={}, name={}, "
                    + "productType={}, status={}, body={}",
                productUuid, productName, productType, ex.getStatusCode(), ex.getResponseBodyAsString());
        } else if (throwable instanceof HttpClientErrorException ex) {
            log.warn(
                "PATCH portfolio product failed: uuid={}, name={}, productType={}, status={}, body={}",
                productUuid, productName, productType, ex.getStatusCode(), ex.getResponseBodyAsString());
        } else {
            log.error("PATCH portfolio product failed: uuid={}, name={}, productType={}",
                productUuid, productName, productType, throwable);
        }
    }

    /**
     * Logs portfolio product creation errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for HTTP client exceptions including
     * HTTP status code and response body.
     *
     * @param productName the name of the portfolio product being created
     * @param productType the product type being created
     * @param throwable   the exception that occurred during creation
     */
    private void logPortfolioProductCreationError(String productName, String productType, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to create portfolio product: name={}, productType={}, status={}, body={}",
                productName, productType, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else if (throwable instanceof HttpClientErrorException ex) {
            log.error("Failed to create portfolio product: name={}, productType={}, status={}, body={}",
                productName, productType, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to create portfolio product: name={}, productType={}",
                productName, productType, throwable);
        }
    }

}
