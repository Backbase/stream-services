package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.integration.product.model.GetProductGroupResponse;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPullRequest;
import reactor.core.publisher.Mono;

public interface ProductIntegrationService {
    Mono<GetProductGroupResponse> retrieveProductGroup(ProductIngestPullRequest ingestPullRequest);
}
