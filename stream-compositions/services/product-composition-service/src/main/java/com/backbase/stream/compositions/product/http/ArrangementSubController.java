package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
class ArrangementSubController {
    private final ArrangementIngestionService arrangementIngestionService;
    private final ArrangementMapper arrangementMapper;

    Mono<ResponseEntity<ArrangementIngestionResponse>> pullIngestArrangement(
            Mono<ArrangementPullIngestionRequest> arrangementPullIngestionRequest, ServerWebExchange exchange) {
        return arrangementPullIngestionRequest
                .map(this::buildPullRequest)
                .flatMap(arrangementIngestionService::ingestPull)
                .map(this::mapArrangementIngestionToResponse);
    }

    Mono<ResponseEntity<ArrangementIngestionResponse>> pushIngestArrangement(
            Mono<ArrangementPushIngestionRequest> arrangementPushIngestionRequest, ServerWebExchange exchange) {
        return arrangementPushIngestionRequest.map(this::buildPushRequest)
                .flatMap(arrangementIngestionService::ingestPush)
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return ProductIngestPushRequest
     */
    private ArrangementIngestPushRequest buildPushRequest(ArrangementPushIngestionRequest request) {
        return ArrangementIngestPushRequest.builder()
                .arrangement(arrangementMapper.mapCompositionToStream(request.getArrangement()))
                .source(request.getSource())
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductCatalogIngestResponse
     * @return IngestionResponse
     */
    private ResponseEntity<ArrangementIngestionResponse> mapIngestionToResponse(ArrangementIngestResponse response) {
        return new ResponseEntity<>(
                new ArrangementIngestionResponse()
                        .withArrangement(
                                arrangementMapper.mapStreamToComposition(response.getArrangement())
                        ),
                HttpStatus.CREATED);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request ArrangementPullIngestionRequest
     * @return ArrangementPullIngestionRequest
     */
    private ArrangementIngestPullRequest buildPullRequest(ArrangementPullIngestionRequest request) {
        return ArrangementIngestPullRequest
                .builder()
                .arrangementId(request.getArrangementId())
                .externalArrangementId(request.getExternalArrangementId())
                .source(request.getSource())
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ArrangementIngestPullResponse
     * @return ArrangementIngestionResponse
     */
    private ResponseEntity<ArrangementIngestionResponse> mapArrangementIngestionToResponse(
            ArrangementIngestResponse response) {
        return new ResponseEntity<>(
                new ArrangementIngestionResponse()
                        .withArrangement(
                                arrangementMapper.mapStreamToComposition(response.getArrangement())),
                HttpStatus.CREATED);
    }
}
