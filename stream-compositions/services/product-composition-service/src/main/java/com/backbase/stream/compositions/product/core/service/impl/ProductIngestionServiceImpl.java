package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.Error;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.compositions.product.core.service.ProductPostIngestionService;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.BatchProductIngestionMode;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIngestionServiceImpl implements ProductIngestionService {
    private final BatchProductIngestionSaga batchProductIngestionSaga;
    private final ProductIntegrationService productIntegrationService;
    private final ProductConfigurationProperties config;
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
        return pushProductGroup(ingestPushRequest)
                .flatMap(this::sendToDbs)
                .flatMap(productPostIngestionService::handleSuccess)
                .doOnError(productPostIngestionService::handleFailure);
    }

    /**
     * Pulls and remap product group from integration service.
     *
     * @param request ProductIngestPullRequest
     * @return ProductGroup
     */
    private Mono<ProductIngestResponse> pullProductGroup(ProductIngestPullRequest request) {
        return productIntegrationService.pullProductGroup(request);
    }

    private Mono<ProductIngestResponse> pushProductGroup(ProductIngestPushRequest request) {
        return Mono.just(
                ProductIngestResponse.builder()
                        .productGroups(Collections.singletonList(request.getProductGroup()))
                        .serviceAgreementExternalId(
                                request.getProductGroup().getServiceAgreement().getExternalId())
                        .serviceAgreementInternalId(
                                request.getProductGroup().getServiceAgreement().getInternalId())
                        .source(request.getSource())
                        .build());
    }

    /**
     * Ingests product group to DBS.
     *
     * @param res Product Ingest Response
     * @return Ingested product group
     */
    private Mono<ProductIngestResponse> sendToDbs(ProductIngestResponse res) {
        return batchProductIngestionSaga
                .process(buildBatchTask(res))
                .map(BatchProductGroupTask::getData)
                .map(
                        pg ->
                                ProductIngestResponse.builder()
                                        .productGroups(
                                                pg.getProductGroups().stream()
                                                        .map(g -> (ProductGroup) g)
                                                        .collect(Collectors.toList()))
                                        .serviceAgreementExternalId(
                                                res.getServiceAgreementExternalId())
                                        .serviceAgreementInternalId(
                                                res.getServiceAgreementInternalId())
                                        .userExternalId(res.getUserExternalId())
                                        .userInternalId(res.getUserInternalId())
                                        .legalEntityExternalId(res.getLegalEntityExternalId())
                                        .legalEntityInternalId(res.getLegalEntityInternalId())
                                        .source(res.getSource())
                                        .additions(res.getAdditions())
                                        .transactionChainEnabledFromRequest(
                                                res.getTransactionChainEnabledFromRequest())
                                        .paymentOrderChainEnabledFromRequest(
                                                res.getPaymentOrderChainEnabledFromRequest())
                                        .build());
    }

    private BatchProductGroupTask buildBatchTask(ProductIngestResponse res) {
        BatchProductGroup bpg = new BatchProductGroup();
        res.getProductGroups().forEach(bpg::addProductGroupsItem);

        bpg.setServiceAgreement(
                new ServiceAgreement()
                        .internalId(res.getServiceAgreementInternalId())
                        .externalId(res.getServiceAgreementExternalId()));

        return new BatchProductGroupTask(
                res.getServiceAgreementInternalId(),
                bpg,
                BatchProductIngestionMode.builder()
                        .functionGroupsMode(config.getIngestionMode().getFunctionGroups())
                        .dataGroupIngestionMode(config.getIngestionMode().getDataGroups())
                        .arrangementsMode(config.getIngestionMode().getArrangements())
                        .build());
    }

    private Mono<ProductIngestResponse> validate(ProductIngestResponse res) {

        if (res.getProductGroups() == null || res.getProductGroups().isEmpty()) {
            log.info("No product groups. Skipping ingestion.");
            return Mono.empty();
        }

        Set<ConstraintViolation<List<ProductGroup>>> violations =
                validator.validate(res.getProductGroups());

        if (!CollectionUtils.isEmpty(violations)) {
            List<Error> errors =
                    violations.stream()
                            .map(
                                    c ->
                                            new Error()
                                                    .withMessage(c.getMessage())
                                                    .withKey(Error.INVALID_INPUT_MESSAGE))
                            .collect(Collectors.toList());
            return Mono.error(new BadRequestException().withErrors(errors));
        }

        return Mono.just(res);
    }
}
