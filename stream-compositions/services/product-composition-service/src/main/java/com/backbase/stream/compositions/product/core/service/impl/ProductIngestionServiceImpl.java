package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.product.BatchProductIngestionSaga;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIngestionServiceImpl implements ProductIngestionService {
    private final ProductGroupMapper mapper;
    private final BatchProductIngestionSaga batchProductIngestionSaga;
    private final ProductIntegrationService productIntegrationService;

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPull(Mono<ProductIngestPullRequest> ingestPullRequest) {
        return ingestPullRequest
                .map(this::pullProductGroup)
                .flatMap(this::sendToDbs)
                .doOnSuccess(this::handleSuccess)
                .map(this::buildResponse);
    }

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPush(Mono<ProductIngestPushRequest> ingestPushRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pulls and remap product group from integration service.
     *
     * @param request ProductIngestPullRequest
     * @return ProductGroup
     */
    private Mono<ProductGroup> pullProductGroup(ProductIngestPullRequest request) {
        return productIntegrationService
                .pullProductGroup(request)
                .map(mapper::mapIntegrationToStream);
    }

    /**
     * Ingests product group to DBS.
     *
     * @param productGroup Product group
     * @return Ingested product group
     */
    private Mono<ProductGroup> sendToDbs(Mono<ProductGroup> productGroup) {
        return productGroup;/*
                .map(ProductGroupTask::new)
                .flatMap(batchProductIngestionSaga::process)
                .map(ProductGroupTask::getData);*/
    }

    private ProductIngestResponse buildResponse(ProductGroup productGroup) {
        return ProductIngestResponse.builder()
                .productGroup(productGroup)
                .build();
    }

    private void handleSuccess(ProductGroup productGroup) {
        log.error("Product group ingestion completed");
        if (log.isDebugEnabled()) {
            log.debug("Product group: {}", productGroup);
        }

    }
}
