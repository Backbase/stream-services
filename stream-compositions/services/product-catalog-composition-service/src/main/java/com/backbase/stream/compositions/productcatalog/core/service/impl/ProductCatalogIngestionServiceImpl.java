package com.backbase.stream.compositions.productcatalog.core.service.impl;

import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPullRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIntegrationService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductCatalogIngestionServiceImpl implements ProductCatalogIngestionService {

    private final ProductCatalogMapper mapper;
    private final ReactiveProductCatalogService reactiveProductCatalogService;
    private final ProductCatalogIntegrationService productCatalogIntegrationService;

    @Override
    public Mono<ProductCatalogIngestResponse> ingestPull(Mono<ProductCatalogIngestPullRequest> ingestPullRequest) {
        return ingestPullRequest
                .map(this::pullProductCatalog)
                .flatMap(this::sendToDbs)
                .doOnSuccess(this::handleSuccess)
                .map(this::buildResponse);
    }

    @Override
    public Mono<ProductCatalogIngestResponse> ingestPush(Mono<ProductCatalogIngestPushRequest> ingestPullRequest) {
        throw new UnsupportedOperationException();
    }

    private Mono<ProductCatalog> pullProductCatalog(ProductCatalogIngestPullRequest ingestPullRequest) {
        return productCatalogIntegrationService
                .pullProductCatalog(ingestPullRequest)
                .map(mapper::mapIntegrationToStream);
    }

    private Mono<ProductCatalog> sendToDbs(Mono<ProductCatalog> productCatalog) {
        return productCatalog
                .flatMap(reactiveProductCatalogService::upsertProductCatalog);
    }

    private ProductCatalogIngestResponse buildResponse(ProductCatalog productCatalog) {
        return ProductCatalogIngestResponse.builder()
                .productCatalog(productCatalog)
                .build();
    }

    private void handleSuccess(ProductCatalog productCatalog) {
        log.info("Product catalog ingestion completed");
        log.debug("Ingested product catalog: {}", productCatalog);
    }
}
