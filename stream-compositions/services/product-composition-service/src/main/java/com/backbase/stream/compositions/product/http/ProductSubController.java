package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
class ProductSubController {
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper productMapper;

    Mono<ResponseEntity<ProductIngestionResponse>> pullIngestProduct(
            @Valid Mono<ProductPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return pullIngestionRequest
                .map(this::buildPullRequest)
                .flatMap(productIngestionService::ingestPull)
                .map(this::mapIngestionToResponse);
    }

    Mono<ResponseEntity<ProductIngestionResponse>> pushIngestProduct(
            @Valid Mono<ProductPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return pushIngestionRequest.map(this::buildPushRequest)
                .flatMap(productIngestionService::ingestPush)
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return ProductIngestPushRequest
     */
    private ProductIngestPushRequest buildPushRequest(ProductPushIngestionRequest request) {
        return ProductIngestPushRequest.builder()
                .productGroup(productMapper.mapCompositionToStream(request.getProductGroup()))
                .source(request.getSource())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return ProductIngestPullRequest
     */
    private ProductIngestPullRequest buildPullRequest(ProductPullIngestionRequest request) {
        return ProductIngestPullRequest
                .builder()
                .legalEntityInternalId(request.getLegalEntityInternalId())
                .legalEntityExternalId(request.getLegalEntityExternalId())
                .serviceAgreementExternalId(request.getServiceAgreementExternalId())
                .serviceAgreementInternalId(request.getServiceAgreementInternalId())
                .userExternalId(request.getUserExternalId())
                .userInternalId(request.getUserInternalId())
                .membershipAccounts(request.getMembershipAccounts())
                .additions(request.getAdditions())
                .referenceJobRoleNames(request.getReferenceJobRoleNames())
                .source(request.getSource())
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductCatalogIngestResponse
     * @return IngestionResponse
     */
    private ResponseEntity<ProductIngestionResponse> mapIngestionToResponse(ProductIngestResponse response) {
        return new ResponseEntity<>(
                new ProductIngestionResponse()
                        .withProductGroups(
                                response.getProductGroups().stream()
                                        .map(productMapper::mapStreamToComposition)
                                        .collect(Collectors.toList())),
                HttpStatus.CREATED);
    }
}
