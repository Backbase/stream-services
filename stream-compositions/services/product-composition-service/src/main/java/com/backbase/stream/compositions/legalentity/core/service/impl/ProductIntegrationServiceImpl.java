package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.GetProductGroupRequest;
import com.backbase.stream.compositions.integration.product.model.GetProductGroupResponse;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.service.ProductIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIntegrationServiceImpl implements ProductIntegrationService {
    private final ProductIntegrationApi productIntegrationApi;

    public Mono<GetProductGroupResponse> retrieveProductGroup(ProductIngestPullRequest ingestPullRequest) {
        return productIntegrationApi.getProductGroup(prepareRequest(ingestPullRequest));
    }

    private GetProductGroupRequest prepareRequest(ProductIngestPullRequest ingestPullRequest) {
        return new GetProductGroupRequest()
                .legalEntityExternalId(ingestPullRequest.getLegalEntityExternalId());
    }
}
