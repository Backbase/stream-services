package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.integration.product.model.GetProductGroupResponse;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import reactor.core.publisher.Mono;

public interface ProductIntegrationService {
    Mono<GetProductGroupResponse> retrieveProductGroup(ProductIngestPullRequest ingestPullRequest);
}
