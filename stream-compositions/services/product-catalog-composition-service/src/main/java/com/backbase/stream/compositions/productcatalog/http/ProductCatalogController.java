package com.backbase.stream.compositions.productcatalog.http;

import com.backbase.stream.compositions.productcatalog.api.ProductCatalogCompositionApi;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPullRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogIngestionResponse;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogPullIngestionRequest;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogPushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public Mono<ResponseEntity<ProductCatalogIngestionResponse>> pullIngestProductCatalog(
            @Valid Mono<ProductCatalogPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return productCatalogIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse);
    }

    @Override
    public Mono<ResponseEntity<ProductCatalogIngestionResponse>> pushIngestProductCatalog(
            @Valid Mono<ProductCatalogPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return productCatalogIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return ProductIngestPullRequest
     */
    private ProductCatalogIngestPullRequest buildPullRequest(ProductCatalogPullIngestionRequest request) {
        return ProductCatalogIngestPullRequest.builder()
                .additionalParameters(request.getAdditionalParameters())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return LegalEntityIngestPushRequest
     */
    private ProductCatalogIngestPushRequest buildPushRequest(ProductCatalogPushIngestionRequest request) {
        return ProductCatalogIngestPushRequest.builder()
                .productCatalog(mapper.mapCompositionToStream(request.getProductCatalog()))
                .build();

    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductCatalogIngestResponse
     * @return IngestionResponse
     */
    private ResponseEntity<ProductCatalogIngestionResponse> mapIngestionToResponse(ProductCatalogIngestResponse response) {
        return new ResponseEntity<>(
                new ProductCatalogIngestionResponse()
                        .withProductCatalog(mapper.mapStreamToComposition(response.getProductCatalog())),
                HttpStatus.CREATED);
    }
}
