package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductRestMapper {
    private final ProductGroupMapper productMapper;

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return ProductIngestPullRequest
     */
    public ProductIngestPullRequest mapPullRequest(ProductPullIngestionRequest request) {
        return ProductIngestPullRequest.builder()
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
    public ProductIngestPushRequest mapPushRequest(ProductPushIngestionRequest request) {
        return ProductIngestPushRequest.builder()
                .productGroup(productMapper.mapCompositionToStream(request.getProductGroup()))
                .source(request.getSource())
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductCatalogIngestResponse
     * @return IngestionResponse
     */
    public ResponseEntity<ProductIngestionResponse> mapResponse(ProductIngestResponse response) {
        return new ResponseEntity<>(
                new ProductIngestionResponse()
                        .withProductGroups(
                                response.getProductGroups().stream()
                                        .map(productMapper::mapStreamToComposition)
                                        .collect(Collectors.toList())),
                HttpStatus.CREATED);
    }
}
