package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.ProductCompositionApi;
import com.backbase.stream.compositions.product.api.model.*;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.*;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@Slf4j
public class ProductController implements ProductCompositionApi {
    private final ProductIngestionService productIngestionService;
    private final ArrangementIngestionService arrangementIngestionService;
    private final ProductGroupMapper productMapper;
    private final ArrangementMapper arrangementMapper;

    @Override
    public Mono<ResponseEntity<ArrangementIngestionResponse>> pullIngestArrangement(
            Mono<ArrangementPullIngestionRequest> arrangementPullIngestionRequest, ServerWebExchange exchange) {
        return arrangementPullIngestionRequest
                .map(this::buildPullRequest)
                .flatMap(arrangementIngestionService::ingestPull)
                .map(this::mapArrangementIngestionToResponse);
    }

    @Override
    public Mono<ResponseEntity<ProductIngestionResponse>> pullIngestProduct(
            @Valid Mono<ProductPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return pullIngestionRequest
                .map(this::buildPullRequest)
                .flatMap(productIngestionService::ingestPull)
                .map(this::mapIngestionToResponse);
    }

    @Override
    public Mono<ResponseEntity<ArrangementIngestionResponse>> pushIngestArrangement(
            Mono<ArrangementPushIngestionRequest> arrangementPushIngestionRequest, ServerWebExchange exchange) {
        return arrangementPushIngestionRequest.map(this::buildPushRequest)
                .flatMap(arrangementIngestionService::ingestPush)
                .map(this::mapIngestionToResponse);
    }


    @Override
    public Mono<ResponseEntity<ProductIngestionResponse>> pushIngestProduct(
            @Valid Mono<ProductPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return pushIngestionRequest.map(this::buildPushRequest)
                .flatMap(productIngestionService::ingestPush)
                .map(this::mapIngestionToResponse);
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
    private ResponseEntity<ProductIngestionResponse> mapIngestionToResponse(ProductIngestResponse response) {
        return new ResponseEntity<>(
                new ProductIngestionResponse()
                        .withProductGroups(
                                response.getProductGroups().stream()
                                        .map(productMapper::mapStreamToComposition)
                                        .collect(Collectors.toList())),
                HttpStatus.CREATED);
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
