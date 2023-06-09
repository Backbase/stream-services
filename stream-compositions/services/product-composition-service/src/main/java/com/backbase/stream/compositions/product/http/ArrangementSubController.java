package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ArrangementRestMapper;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
class ArrangementSubController {

    private final ArrangementIngestionService arrangementIngestionService;
    private final ArrangementRestMapper arrangementRestMapper;

    Mono<ResponseEntity<ArrangementIngestionResponse>> pullIngestArrangement(
        Mono<ArrangementPullIngestionRequest> arrangementPullIngestionRequest,
        ServerWebExchange exchange) {
        return arrangementPullIngestionRequest
            .map(arrangementRestMapper::mapPullRequest)
            .flatMap(arrangementIngestionService::ingestPull)
            .map(arrangementRestMapper::mapResponse);
    }

    Mono<ResponseEntity<ArrangementIngestionResponse>> pushIngestArrangement(
        Mono<ArrangementPushIngestionRequest> arrangementPushIngestionRequest,
        ServerWebExchange exchange) {
        return arrangementPushIngestionRequest
            .map(arrangementRestMapper::mapPushRequest)
            .flatMap(arrangementIngestionService::ingestPush)
            .map(arrangementRestMapper::mapResponse);
    }
}
