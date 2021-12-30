package com.backbase.stream.productcatalog;

import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.dbs.arrangement.api.service.v2.ProductKindsApi;
import com.backbase.dbs.arrangement.api.service.v2.ProductsApi;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountProductId;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountProductKindId;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountSchemasProductItem;
import com.backbase.dbs.arrangement.api.service.v2.model.ExternalProductKindItemExtended;
import com.backbase.dbs.arrangement.api.service.v2.model.ExternalProductKindItemPut;
import com.backbase.stream.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reactive Product Catalog Service allowing to setup a complete Product Catalog in a single call.
 */
@Slf4j
public class ReactiveProductCatalogService {

    private final ProductsApi productsApi;
    private final ProductKindsApi productKindsApi;
    private final ProductCatalogMapper productCatalogMapper = Mappers.getMapper(ProductCatalogMapper.class);

    public ReactiveProductCatalogService(ApiClient accountPresentationClient) {
        this.productsApi = new ProductsApi(accountPresentationClient);
        this.productKindsApi = new ProductKindsApi(accountPresentationClient);
    }

    /**
     * Get Product Catalog from DBS.
     *
     * @return Product Catalog
     */
    public Mono<ProductCatalog> getProductCatalog() {
        Flux<ProductKind> productKindsFlux = getProductKindFlux();

        Flux<AccountSchemasProductItem> products = productsApi.getProducts(null, null);
        Flux<ProductType> productTypesFlux = products.map(productCatalogMapper::toStream);

        return Mono.zip(productKindsFlux.collectList(), productTypesFlux.collectList())
            .map(tuple -> new ProductCatalog().productTypes(tuple.getT2()).productKinds(tuple.getT1()))
            .doOnNext(productCatalog ->  log.info("Created Product Catalog"));
    }

    private Flux<ProductKind> getProductKindFlux() {
        return productKindsApi.getProductKinds(null, null, null)
            .flux()
            .flatMap(productKindsWrapper -> Flux.fromIterable(productKindsWrapper.getProductKinds() != null
                ? productKindsWrapper.getProductKinds()
                : new ArrayList<>()))
            .map(productCatalogMapper::toStream);
    }

    /**
     * Setup Product Catalog from Aggregate.
     *
     * @param productCatalog Product Catalog
     * @return Completed Product Catalog
     */
    public Mono<ProductCatalog> setupProductCatalog(ProductCatalog productCatalog) {
        return getProductCatalog().flatMap(existingProductCatalog -> {

            List<ProductKind> newProductKinds = new ArrayList<>();
            if (productCatalog.getProductKinds() != null) {
                newProductKinds = productCatalog.getProductKinds().stream()
                    .filter(newProductKind -> existingProductCatalog.getProductKinds().stream()
                        .noneMatch(productKind ->
                            productKind.getExternalKindId().equals(newProductKind.getExternalKindId())))
                    .collect(Collectors.toList());
            }
            List<ProductType> newProductTypes = new ArrayList<>();
            if (productCatalog.getProductTypes() != null) {
                newProductTypes = productCatalog.getProductTypes().stream()
                    .filter(newProductType -> existingProductCatalog.getProductTypes().stream()
                        .noneMatch(productType ->
                            productType.getExternalProductId().equals(newProductType.getExternalProductId())))
                    .collect(Collectors.toList());
            }

            Flux<ProductKind> productKindFlux = storeProductKinds(newProductKinds)
                    .mergeWith(getProductKindFlux());

            // Ensure products kinds are created first
            List<ProductType> finalNewProductTypes = newProductTypes;
            return productKindFlux.collectList().flatMap(productKinds -> {
                return createProductTypes(finalNewProductTypes, productKinds).collectList().flatMap(productTypes -> {
                    productCatalog.setProductTypes(productTypes);
                    return Mono.just(productCatalog);
                });
            });
        });
    }



    /**
     * Setup Product Catalog from Aggregate.
     *
     * @param productCatalog Product Catalog
     * @return Completed Product Catalog
     */
    public Mono<ProductCatalog> updateExistingProductCatalog(ProductCatalog productCatalog) {
        return getProductCatalog().flatMap(existingProductCatalog -> {

            List<ProductKind> existingProductKinds = productCatalog.getProductKinds().stream()
                    .filter(existingProductKind -> existingProductCatalog.getProductKinds().stream()
                            .anyMatch(productKind ->
                                    productKind.getExternalKindId().equals(existingProductKind.getExternalKindId())))
                    .collect(Collectors.toList());
            List<ProductType> existingProductTypes = productCatalog.getProductTypes().stream()
                    .filter(existingProductType -> existingProductCatalog.getProductTypes().stream()
                            .anyMatch(productType ->
                                    productType.getExternalProductId().equals(existingProductType.getExternalProductId())))
                    .collect(Collectors.toList());

            Flux<ProductKind> existingProductKindFlux = updateProductKind(existingProductKinds).map(productCatalogMapper::toStream)
                    .map(productCatalogMapper::toStream).mergeWith(getProductKindFlux());

            return existingProductKindFlux.collectList().flatMap(
                    productKinds -> {
                        return updateProductTypes(existingProductTypes, productKinds).collectList().flatMap(productTypes -> {
                            return Mono.just(productCatalog);
                        });
                    }
            );
        });
    }


    private Flux<ExternalProductKindItemPut> updateProductKind(List<ProductKind> productKinds) {
        log.info("Updating Product Type1: {}", productKinds);
        return Flux.fromIterable(productKinds)
                .map(productCatalogMapper::toPresentation)
                .map(productCatalogMapper::toPutPresentation)
                .flatMap(this::updateProductKind);
    }

    private Mono<ExternalProductKindItemPut> updateProductKind(ExternalProductKindItemPut productKind) {
        log.info("Updating Product Type2: {}", productKind.getKindName());
        return productKindsApi.putProductKinds(productKind)
                .doOnError(WebClientResponseException.BadRequest.class, e ->
                        log.error("Bad Request Storing Product Kind: {} \n[{}]: {}\nResponse: {}", productKind, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())
                )
                .doOnError(WebClientResponseException.class, e ->
                        log.error("Bad Request Product Kind: {} \n[{}]: {}\nResponse: {}", productKind, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())
                ).map(actual -> productKind);
    }

    private Flux<ProductType> createProductTypes(List<ProductType> productTypes, List<ProductKind> productKinds) {
        return Flux.fromIterable(productTypes)
            .flatMap(productType -> createProductType(productType, productKinds));
    }

    private Flux<ProductType> updateProductTypes(List<ProductType> productTypes, List<ProductKind> productKinds) {
        return Flux.fromIterable(productTypes)
                .flatMap(productType -> updateProductType(productType, productKinds));
    }

    public Mono<ProductType> updateProductType(ProductType productType, List<ProductKind> productKinds) {
        Mono<Void> productIdMono = Mono.just(productType)
                .map(productCatalogMapper::toPresentation)
                .map(productItem -> {
                    log.info("Updating Product Type: {}", productItem.getTypeName());
                    ProductKind productKind;
                    if (productItem.getProductKind() != null) {
                        productKind = productType.getProductKind();
                    } else {
                        productKind = productKinds.stream()
                                .filter(kind -> productType.getExternalProductKindId().equals(kind.getExternalKindId()))
                                .findFirst()
                                .orElseThrow(NullPointerException::new);
                    }
                    productItem.setExternalProductKindId(productKind.getExternalKindId());
                    productItem.setProductKindName(productKind.getKindName());
                    productItem.setProductTypeName(productType.getTypeName());
                    productItem.setExternalTypeId(productType.getExternalTypeId());
                    productItem.setAdditions(productType.getAdditions());
                    return productItem;
                })
                .flatMap(
                        productItem ->
                                productsApi.putProducts(productItem)
                                        .doOnError(WebClientResponseException.BadRequest.class, e -> log.error("Bad Request Storing Product Type: {} \n[{}]: {}\nResponse: {}", productItem, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString()))
                                        .doOnError(WebClientResponseException.class, e -> log.error("Bad Request Storing Product Type: {} \n[{}]: {}\nResponse: {}", productItem, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())));

        return Mono.zip(Mono.just(productType), productIdMono, this::handelUpdateProductTypeResult);
    }


    public Mono<ProductType> createProductType(ProductType productType, List<ProductKind> productKinds) {

        Mono<AccountProductId> productIdMono = Mono.just(productType)
            .map(productCatalogMapper::toPresentation)
            .map(productItem -> {
                log.info("Creating Product Type: {}", productItem.getProductTypeName());
                ProductKind productKind;
                if (productItem.getProductKind() != null) {
                    productKind = productType.getProductKind();
                } else {
                    productKind = productKinds.stream()
                        .filter(kind -> productType.getExternalProductKindId().equals(kind.getExternalKindId()))
                        .findFirst()
                        .orElseThrow(NullPointerException::new);
                }
                productItem.setExternalProductKindId(productKind.getExternalKindId());
                productItem.setProductKindName(productKind.getKindName());
                productItem.setProductTypeName(productType.getTypeName());
                productItem.setExternalTypeId(productType.getExternalTypeId());
                productItem.setAdditions(productType.getAdditions());
                return productItem;
            })
            .flatMap(
                productItem ->
                    productsApi.postProducts(productItem)
                        .doOnError(WebClientResponseException.BadRequest.class, e -> log.error("Bad Request Storing Product Type: {} \n[{}]: {}\nResponse: {}", productItem, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString()))
                        .doOnError(WebClientResponseException.class, e -> log.error("Bad Request Storing Product Type: {} \n[{}]: {}\nResponse: {}", productItem, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())));

        return Mono.zip(Mono.just(productType), productIdMono, this::handelStoreProductTypeResult);
    }


    private ProductType handelStoreProductTypeResult(ProductType productType, AccountProductId productId) {
        log.info("Product Type: {} created with: {}", productType.getProductTypeName(), productId.getId());
        return productType;
    }

    private ProductType handelUpdateProductTypeResult(ProductType productType, Object productIdMono) {
        log.info("Product Type: {} updated.", productType.getProductTypeName());
        return productType;
    }
    private Flux<ProductKind> storeProductKinds(List<ProductKind> productKinds) {
        return Flux.fromIterable(productKinds)
            .map(productCatalogMapper::toPresentation)
            .flatMap(this::storeProductKind);
    }


    private Mono<ProductKind> storeProductKind(ExternalProductKindItemExtended productKind) {
        Mono<AccountProductKindId> productKindIdMono = productKindsApi.postProductKinds(productKind)
            .doOnError(WebClientResponseException.BadRequest.class, e ->
                log.error("Bad Request Storing Product Kind: {} \n[{}]: {}\nResponse: {}", productKind, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())
            )
            .doOnError(WebClientResponseException.class, e ->
                log.error("Bad Request Product Kind: {} \n[{}]: {}\nResponse: {}", productKind, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())
            );
        return Mono.zip(Mono.just(productKind), productKindIdMono,
            this::handleStoreResult).map(productCatalogMapper::toStream);
    }

    private ExternalProductKindItemExtended handleStoreResult(ExternalProductKindItemExtended productKindItem, AccountProductKindId productKindId) {
        log.info("Product Kind: {} created with: {}", productKindItem.getKindName(), productKindId);
        return productKindItem;
    }


    public Mono<ProductType> getProductTypeByExternalId(String productTypeExternalId) {
        log.info("Get Product Type: {}", productTypeExternalId);
        return productsApi.getProducts(Collections.singletonList(productTypeExternalId), null)
            .doOnNext(productItem -> log.info("Found product: {} for id: {}", productItem.getTypeName(), productTypeExternalId))
            .doOnError(WebClientResponseException.class, ex -> {
                log.error("Failed to get product type by external id: {}. Response: {}", productTypeExternalId, ex.getResponseBodyAsString());
            })
            .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                log.info("No product type found with id: {}", productTypeExternalId);
                return Mono.empty();
            })
            .singleOrEmpty()
            .map(productCatalogMapper::toStream);
    }
}
