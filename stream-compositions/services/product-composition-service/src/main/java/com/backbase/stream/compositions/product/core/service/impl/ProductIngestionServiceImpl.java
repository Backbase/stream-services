package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.product.ProductIngestionSaga;
import com.backbase.stream.product.task.ProductGroupTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIngestionServiceImpl implements ProductIngestionService {
    private final ProductGroupMapper mapper;
    private final ProductIngestionSaga productIngestionSaga;
    private final ProductIntegrationService productIntegrationService;

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPull(Mono<ProductIngestPullRequest> ingestPullRequest) {
        return ingestPullRequest
                .flatMap(productIntegrationService::retrieveProductGroup)
                .map(item -> mapper.mapIntegrationToStream(item.getProductGroup()))
                .flatMap(this::sendProductGroupToDbs)
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
     * Ingests product group to DBS.
     *
     * @param productGroup Product group
     * @return Ingested legal entities
     */
    private Mono<ProductGroup> sendProductGroupToDbs(ProductGroup productGroup) {
        return Mono.just(productGroup)
                .map(ProductGroupTask::new)
                .flatMap(productIngestionSaga::process)
                .map(ProductGroupTask::getData);
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
