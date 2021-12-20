package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.api.ProductCompositionApi;
import com.backbase.stream.compositions.product.model.IngestionResponse;
import com.backbase.stream.compositions.product.model.PullIngestionRequest;
import com.backbase.stream.compositions.product.model.PushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
public class ProductController implements ProductCompositionApi {
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<IngestionResponse>> pullIngestProductGroup(
            @Valid Mono<PullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return productIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse)
                .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<IngestionResponse>> pushIngestProductGroup(
            @Valid Mono<PushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return productIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .map(this::mapIngestionToResponse)
                .map(ResponseEntity::ok);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return ProductIngestPullRequest
     */
    private ProductIngestPullRequest buildPullRequest(PullIngestionRequest request) {
        return ProductIngestPullRequest
                .builder()
                .legalEntityExternalId(request.getLegalEntityExternalId()).build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return ProductIngestPushRequest
     */
    private ProductIngestPushRequest buildPushRequest(PushIngestionRequest request) {
        return ProductIngestPushRequest.builder()
                .productGroup(mapper.mapCompostionToStream(request.getProductGgroup()))
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductIngestResponse
     * @return IngestionResponse
     */
    private IngestionResponse buildResponse(ProductIngestResponse response) {
        return new IngestionResponse()
                .withProductGgroup(mapper.mapStreamToComposition(response.getProductGroup()));
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductIngestResponse
     * @return IngestionResponse
     */
    private IngestionResponse mapIngestionToResponse(ProductIngestResponse response) {
        return new IngestionResponse()
                .withProductGgroup(this.mapper.mapStreamToComposition(response.getProductGroup()));
    }
}
