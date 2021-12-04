package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestResponse;
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
public class ProductIngestionService {
    private final ProductGroupMapper mapper;
    private final ProductIngestionSaga productIngestionSaga;
    private final ProductIntegrationService productIntegrationService;

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPull(ProductIngestPullRequest ingestPullRequest) {
        return ingest(productIntegrationService
                .retrieveProductGroup(ingestPullRequest)
                .map(item -> mapper.mapIntegrationToStream(item.getProductGroup())));
    }

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPush(ProductIngestPushRequest ingestPushRequest) {
        return ingest(Mono.just(ingestPushRequest.getProductGroup()));
    }

    /**
     * Ingests product group to DBS.
     *
     * @param productGroup Product group
     * @return Ingested product group
     */
    private Mono<ProductIngestResponse> ingest(Mono<ProductGroup> productGroup) {
        return productGroup
                .flatMap(this::sendLegalEntityToDbs)
                .doOnSuccess(this::handleSuccess)
                .doOnError(this::handleError)
                .map(this::buildResponse);
    }

    /**
     * Ingests product group to DBS.
     *
     * @param productGroup Product group
     * @return Ingested legal entities
     */
    private Mono<ProductGroup> sendLegalEntityToDbs(ProductGroup productGroup) {
        return Mono.just(productGroup)
                .map(ProductGroupTask::new)
                .flatMap(productIngestionSaga::process)
                .map(item -> item.getData());
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

    private void handleError(Throwable ex) {
        log.error("Product group ingestion failed", ex);
    }
}
