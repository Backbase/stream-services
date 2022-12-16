package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.stream.compositions.integration.product.api.ArrangementIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.PullArrangementRequest;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ArrangementIntegrationServiceImpl implements ArrangementIntegrationService {
    private final ArrangementIntegrationApi arrangementIntegrationApi;
    private final ArrangementMapper arrangementMapper;

    @Override
    public Mono<ArrangementIngestResponse> pullArrangement(
            ArrangementIngestPullRequest ingestionRequest) {
        return arrangementIntegrationApi.pullArrangement(
                        new PullArrangementRequest()
                                .arrangementId(ingestionRequest.getArrangementId())
                                .arrangementExternalId(ingestionRequest.getExternalArrangementId()))
                .map(item -> arrangementMapper.mapIntegrationToStream(item.getArrangement()))
                .map(item -> ArrangementIngestResponse.builder()
                        .arrangement(item)
                        .build())
                .onErrorResume(this::handleIntegrationError)
                .flatMap(this::handleIntegrationResponse);
    }

    private Mono<ArrangementIngestResponse> handleIntegrationResponse(ArrangementIngestResponse res) {
        log.debug("Arrangement from Integration: {}", res.getArrangement());
        return Mono.just(res);
    }

    private Mono<ArrangementIngestResponse> handleIntegrationError(Throwable e) {
        log.error("Error while pulling arrangement: {}", e.getMessage());
        return Mono.error(new InternalServerErrorException().withMessage(e.getMessage()));
    }
}
