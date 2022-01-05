package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.integration.product.model.PullProductGroupResponse;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIntegrationServiceImpl implements ProductIntegrationService {
    private final ProductIntegrationApi productIntegrationApi;

    /**
     * {@inheritDoc}
     */
    public Mono<ProductGroup> pullProductGroup(ProductIngestPullRequest ingestPullRequest) {
        return productIntegrationApi
                .pullProductGroup(
                        ingestPullRequest.getLegalEntityExternalId(),
                        ingestPullRequest.getServiceAgreementExternalId(),
                        ingestPullRequest.getServiceAgreementInternalId(),
                        ingestPullRequest.getUserExternalId(),
                        ingestPullRequest.getAdditionalParameters())
                .map(PullProductGroupResponse::getProductGroup);
    }
}
