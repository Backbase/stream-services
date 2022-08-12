package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.Error;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.compositions.product.core.service.ProductPostIngestionService;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
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
                .flatMap(productPostIngestionService::handleSuccess)
                .doOnError(productPostIngestionService::handleFailure);
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
                .pullProductGroup(request)
                .map(i -> i.toBuilder()
                        .legalEntityExternalId(request.getLegalEntityExternalId())
                        .legalEntityInternalId(request.getLegalEntityInternalId())
                        .userExternalId(request.getUserExternalId())
                        .userInternalId(request.getUserInternalId())
                        .build());
    }

    /**
     * Ingests product group to DBS.
     *
     * @param res Product Ingest Response
     * @return Ingested product group
     */
    private Mono<ProductIngestResponse> sendToDbs(ProductIngestResponse res) {
       return batchProductIngestionSaga.process(buildBatchTask(res.getProductGroup()))
                .map(BatchProductGroupTask::getData)
                .map(pg -> ProductIngestResponse.builder()
                        .productGroup((ProductGroup) pg.getProductGroups().get(0))
                        .build());
    }

    private BatchProductGroupTask buildBatchTask(ProductGroup productGroup) {
        BatchProductGroup bpg = new BatchProductGroup();
        bpg.addProductGroupsItem(productGroup);
        bpg.setServiceAgreement(productGroup.getServiceAgreement());
        return new BatchProductGroupTask(productGroup.getServiceAgreement().getInternalId(),
                bpg, BatchProductGroupTask.IngestionMode.UPDATE);
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
