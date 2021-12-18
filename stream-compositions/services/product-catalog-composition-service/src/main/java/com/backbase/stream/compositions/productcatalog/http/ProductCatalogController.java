package com.backbase.stream.compositions.productcatalog.http;

import com.backbase.stream.compositions.productcatalog.api.ProductCatalogCompositionApi;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.compositions.productcatalog.model.IngestionResponse;
import com.backbase.stream.compositions.productcatalog.model.PullIngestionRequest;
import com.backbase.stream.compositions.productcatalog.model.PushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
public class ProductCatalogController implements ProductCatalogCompositionApi {
    private final ProductCatalogIngestionService productCatalogIngestionService;
    private final ProductCatalogMapper mapper;

    @Override
    public Mono<ResponseEntity<IngestionResponse>> pullIngestProductCatalog(
            @Valid Mono<PullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return productCatalogIngestionService
                .ingestPull()
                .map(this::mapIngestionToResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<IngestionResponse>> pushIngestProductCatalog(
            @Valid Mono<PushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return productCatalogIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .map(this::mapIngestionToResponse)
                .map(ResponseEntity::ok);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return LegalEntityIngestPushRequest
     */
    private ProductCatalogIngestPushRequest buildPushRequest(PushIngestionRequest request) {
        return ProductCatalogIngestPushRequest.builder()
                .productCatalog(mapper.mapCompostionToStream(request.getProductCatalog()))
                .build();

    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductCatalogIngestResponse
     * @return IngestionResponse
     */
    private IngestionResponse mapIngestionToResponse(ProductCatalogIngestResponse response) {
        return new IngestionResponse()
                .withProductCatalog(mapper.mapStreamToComposition(response.getProductCatalog()));
    }
}
