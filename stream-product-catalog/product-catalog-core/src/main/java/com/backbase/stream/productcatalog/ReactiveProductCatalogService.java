package com.backbase.stream.productcatalog;

import com.backbase.dbs.arrangement.api.service.v3.ProductKindsApi;
import com.backbase.dbs.arrangement.api.service.v3.ProductsApi;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementProductsListResponse;
import com.backbase.dbs.arrangement.api.service.v3.model.ResourceAddPostResponse;
import com.backbase.stream.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Product Catalog Service allowing to setup a complete Product Catalog in a single call.
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveProductCatalogService {

    private final ProductsApi productsApi;
    private final ProductKindsApi productKindsApi;
    private final ProductCatalogMapper productCatalogMapper = Mappers.getMapper(ProductCatalogMapper.class);

    /**
     * Get Product Catalog from DBS.
     *
     * @return Product Catalog
     */
    public Mono<ProductCatalog> getProductCatalog() {
        Flux<ProductKind> productKindsFlux = getProductKindFlux();

        Flux<ProductType> productTypesFlux = productsApi.getProducts(null, null)
                                                        .flatMapIterable(ArrangementProductsListResponse::getArrangementProductElements)
                                                        .map(productCatalogMapper::toStream);

        return Mono.zip(productKindsFlux.collectList(), productTypesFlux.collectList())
            .map(tuple -> new ProductCatalog().productTypes(tuple.getT2()).productKinds(tuple.getT1()))
            .doOnNext(productCatalog ->  log.info("Created Product Catalog"));
    }

    private Flux<ProductKind> getProductKindFlux() {
        return productKindsApi.getProductKinds(null, null, null)
            .flux()
            .flatMap(productKindsWrapper -> Flux.fromIterable(productKindsWrapper.getProductKindElements()))
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
            List<ProductType> newProductTypes = new ArrayList<>();
            if (productCatalog.getProductTypes() != null) {
                newProductTypes = productCatalog.getProductTypes().stream()
                    .filter(newProductType -> existingProductCatalog.getProductTypes().stream()
                        .noneMatch(productType ->
                            productType.getExternalProductId().equals(newProductType.getExternalProductId()))).toList();
            }

            List<ProductType> finalNewProductTypes = newProductTypes;
            return getProductKindFlux().collectList().flatMap(productKinds -> createProductTypes(finalNewProductTypes, productKinds).collectList().flatMap(productTypes -> {
                productCatalog.setProductTypes(productTypes);
                return Mono.just(productCatalog);
            }));
        });
    }

    /**
     * Setup Product Catalog from Aggregate. It does not update Product Kinds since Product Kinds
     * are static and should be managed through YAML settings file.
     *
     * @param productCatalog Product Catalog
     * @return Completed Product Catalog
     */
    public Mono<ProductCatalog> updateExistingProductCatalog(ProductCatalog productCatalog) {
        return getProductCatalog().flatMap(existingProductCatalog -> {

            List<ProductType> existingProductTypes = productCatalog.getProductTypes().stream()
                    .filter(existingProductType -> existingProductCatalog.getProductTypes().stream()
                            .anyMatch(productType ->
                                    productType.getExternalProductId().equals(existingProductType.getExternalProductId())))
                    .toList();
            return getProductKindFlux().collectList().flatMap(
                productKinds -> updateProductTypes(existingProductTypes, productKinds).collectList()
                    .flatMap(productTypes -> Mono.just(productCatalog)));
        });
    }

    public Mono<ProductCatalog> upsertProductCatalog(ProductCatalog productCatalog) {
        return updateExistingProductCatalog(productCatalog)
                .flatMap(this::setupProductCatalog);
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
        Mono<ResourceAddPostResponse> arrangementProductItemBaseMono = Mono.just(productType).map(productCatalogMapper::toPresentation)
            .map(arrangementProductItemBase -> {
                log.info("Creating Product Type: {}", arrangementProductItemBase.getTypeName());
                ProductKind productKind;
                if (arrangementProductItemBase.getProductKind() != null) {
                    productKind = productType.getProductKind();
                } else {
                    productKind = productKinds.stream().filter(kind -> productType.getExternalProductKindId().equals(kind.getExternalKindId()))
                        .findFirst()
                        .orElseThrow(NullPointerException::new);
                }

                arrangementProductItemBase.setExternalProductKindId(productKind.getExternalKindId());
                arrangementProductItemBase.setProductKindName(productKind.getKindName());
                arrangementProductItemBase.setProductTypeName(productType.getTypeName());
                arrangementProductItemBase.setExternalTypeId(productType.getExternalTypeId());

                return arrangementProductItemBase;
            })
            .flatMap(arrangementProductItemBase -> productsApi.postProducts(arrangementProductItemBase)
                .doOnError(WebClientResponseException.BadRequest.class, e -> log.error("Bad Request Storing Product Type: {} \n[{}]: {}\nResponse: {}", arrangementProductItemBase, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString()))
                .doOnError(WebClientResponseException.class, e -> log.error("Bad Request Storing Product Type: {} \n[{}]: {}\nResponse: {}", arrangementProductItemBase, Objects.requireNonNull(e.getRequest()).getMethod(), e.getRequest().getURI(), e.getResponseBodyAsString())));

        return Mono.zip(Mono.just(productType), arrangementProductItemBaseMono, this::handelStoreProductTypeResult);
    }

    private ProductType handelStoreProductTypeResult(ProductType productType, ResourceAddPostResponse resourceAddPostResponse) {
        log.info("Product Type: {} created with: {}", productType.getProductTypeName(), resourceAddPostResponse.getId());
        return productType;
    }

    private ProductType handelUpdateProductTypeResult(ProductType productType, Object productIdMono) {
        log.info("Product Type: {} updated.", productType.getProductTypeName());
        return productType;
    }

    public Mono<ProductType> getProductTypeByExternalId(String productTypeExternalId) {
        log.info("Get Product Type: {}", productTypeExternalId);
        return productsApi.getProducts(Collections.singleton(productTypeExternalId), null)
            .flatMapIterable(ArrangementProductsListResponse::getArrangementProductElements)
            .doOnNext(productItem -> log.info("Found product: {} for id: {}", productItem.getTypeName(), productTypeExternalId))
            .doOnError(WebClientResponseException.class, ex ->
                log.error("Failed to get product type by external id: {}. Response: {}", productTypeExternalId, ex.getResponseBodyAsString()))
            .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                log.info("No product type found with id: {}", productTypeExternalId);
                return Mono.empty();
            })
            .singleOrEmpty()
            .map(productCatalogMapper::toStream);
    }
}
