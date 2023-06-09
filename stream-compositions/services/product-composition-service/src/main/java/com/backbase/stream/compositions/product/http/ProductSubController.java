package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ProductRestMapper;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
class ProductSubController {

    private final ProductIngestionService productIngestionService;
    private final ProductRestMapper productRestMapper;

    Mono<ResponseEntity<ProductIngestionResponse>> pullIngestProduct(
        @Valid Mono<ProductPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return pullIngestionRequest
            .map(productRestMapper::mapPullRequest)
            .flatMap(productIngestionService::ingestPull)
            .map(productRestMapper::mapResponse);
    }

    Mono<ResponseEntity<ProductIngestionResponse>> pushIngestProduct(
        @Valid Mono<ProductPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return pushIngestionRequest
            .map(productRestMapper::mapPushRequest)
            .flatMap(productIngestionService::ingestPush)
            .map(productRestMapper::mapResponse);
    }
}
