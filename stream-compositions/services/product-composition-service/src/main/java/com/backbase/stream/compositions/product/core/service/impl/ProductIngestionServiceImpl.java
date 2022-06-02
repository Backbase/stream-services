package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.Error;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.compositions.product.core.service.ProductPostIngestionService;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.ProductGroupTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIngestionServiceImpl implements ProductIngestionService {
    private final BatchProductIngestionSaga batchProductIngestionSaga;
    private final ProductIntegrationService productIntegrationService;

    private final Validator validator;

    private final ProductPostIngestionService productPostIngestionService;

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPull(ProductIngestPullRequest ingestPullRequest) {
        return pullProductGroup(ingestPullRequest)
                .flatMap(this::validate)
                .flatMap(this::sendToDbs)
                .doOnSuccess(productPostIngestionService::handleSuccess)
                .onErrorResume(productPostIngestionService::handleFailure);
    }

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    public Mono<ProductIngestResponse> ingestPush(ProductIngestPushRequest ingestPushRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pulls and remap product group from integration service.
     *
     * @param request ProductIngestPullRequest
     * @return ProductGroup
     */
    private Mono<ProductIngestResponse> pullProductGroup(ProductIngestPullRequest request) {
        return productIntegrationService
                .pullProductGroup(request);
    }

    /**
     * Ingests product group to DBS.
     *
     * @param res Product Ingest Response
     * @return Ingested product group
     */
    private Mono<ProductIngestResponse> sendToDbs(ProductIngestResponse res) {
        return batchProductIngestionSaga.process(new ProductGroupTask(res.getProductGroup()))
                .map(ProductGroupTask::getData)
                .map(pg -> ProductIngestResponse.builder()
                        .productGroup(pg)
                        .build());
    }

    private ProductIngestResponse buildResponse(ProductGroup productGroup) {
        return ProductIngestResponse.builder()
                .productGroup(productGroup)
                .build();
    }

    private void handleSuccess(ProductGroup productGroup) {
        log.info("Product group ingestion completed");
        if (log.isDebugEnabled()) {
            log.debug("Product group: {}", productGroup);
        }

    }

    private Mono<ProductIngestResponse> validate(ProductIngestResponse res) {
        Set<ConstraintViolation<ProductGroup>> violations = validator.validate(res.getProductGroup());

        if (!CollectionUtils.isEmpty(violations)) {
            List<Error> errors = violations.stream().map(c -> new Error()
                    .withMessage(c.getMessage())
                    .withKey(Error.INVALID_INPUT_MESSAGE)).collect(Collectors.toList());
            return Mono.error(new BadRequestException().withErrors(errors));
        }

        return Mono.just(res);
    }
}
