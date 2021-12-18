package com.backbase.stream.compositions.productcatalog.core.service.impl;

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
    public Mono<ProductCatalogIngestResponse> ingestPull() {
        return productCatalogIntegrationService
                .retrieveProductCatalog()
                .map(mapper::mapIntegrationToStream)
                .flatMap(reactiveProductCatalogService::setupProductCatalog)
                .doOnSuccess(this::handleSuccess)
                .map(this::buildResponse);
    }

    @Override
    public Mono<ProductCatalogIngestResponse> ingestPush(Mono<ProductCatalogIngestPushRequest> ingestPullRequest) {
        return ingestPullRequest
                .map(item -> item.getProductCatalog())
                .flatMap(reactiveProductCatalogService::setupProductCatalog)
                .map(this::buildResponse);
    }

    private ProductCatalogIngestResponse buildResponse(ProductCatalog productCatalog) {
        return ProductCatalogIngestResponse.builder()
                .productCatalog(productCatalog)
                .build();
    }

    private void handleSuccess(ProductCatalog productCatalog) {
        log.info("Product catalog ingestion completed");
        if (log.isDebugEnabled()) {
            log.debug("Ingested product catalog: {}", productCatalog);
        }
    }
}
