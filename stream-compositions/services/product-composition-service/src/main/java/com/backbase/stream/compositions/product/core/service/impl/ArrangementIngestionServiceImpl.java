package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.Error;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import com.backbase.stream.compositions.product.core.service.ArrangementIntegrationService;
import com.backbase.stream.compositions.product.core.service.ArrangementPostIngestionService;
import com.backbase.stream.product.service.ArrangementService;
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
public class ArrangementIngestionServiceImpl implements ArrangementIngestionService {
    private final ArrangementService arrangementService;
    private final ArrangementIntegrationService arrangementIntegrationService;
    private final ArrangementPostIngestionService arrangementPostIngestionService;
    private final Validator validator;

    @Override
    public Mono<ArrangementIngestResponse> ingestPull(
            ArrangementIngestPullRequest ingestionRequest) {
        return pullArrangement(ingestionRequest)
                .flatMap(this::validate)
                .flatMap(this::sendToDbs)
                .flatMap(arrangementPostIngestionService::handleSuccess)
                .doOnError(arrangementPostIngestionService::handleFailure);
    }

    @Override
    public Mono<ArrangementIngestResponse> ingestPush(ArrangementIngestPushRequest request) {
        return pushArrangement(request)
                .flatMap(this::sendToDbs)
                .flatMap(arrangementPostIngestionService::handleSuccess)
                .doOnError(arrangementPostIngestionService::handleFailure);
    }

    /**
     * Pulls arrangement from integration service.
     *
     * @param request ArrangementPullIngestionRequest
     * @return ProductGroup
     */
    private Mono<ArrangementIngestResponse> pullArrangement(ArrangementIngestPullRequest request) {
        return arrangementIntegrationService.pullArrangement(request);
    }

    public Mono<ArrangementIngestResponse> pushArrangement(ArrangementIngestPushRequest request) {
        return Mono.just(ArrangementIngestResponse.builder()
                .arrangement(request.getArrangement())
                .source(request.getSource())
                .build());
    }

    private Mono<ArrangementIngestResponse> validate(ArrangementIngestResponse res) {
        Set<ConstraintViolation<AccountArrangementItemPut>> violations = validator.validate(res.getArrangement());

        if (!CollectionUtils.isEmpty(violations)) {
            List<Error> errors = violations.stream().map(c -> new Error()
                    .withMessage(c.getMessage())
                    .withKey(Error.INVALID_INPUT_MESSAGE)).collect(Collectors.toList());
            return Mono.error(new BadRequestException().withErrors(errors));
        }

        return Mono.just(res);
    }

    private Mono<ArrangementIngestResponse> sendToDbs(ArrangementIngestResponse res) {
        return arrangementService.updateArrangement(res.getArrangement())
                .map(item -> ArrangementIngestResponse.builder()
                        .arrangement(res.getArrangement())
                        .build());
    }
}
