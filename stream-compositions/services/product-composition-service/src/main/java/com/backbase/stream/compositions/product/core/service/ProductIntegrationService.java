package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import reactor.core.publisher.Mono;

public interface ProductIntegrationService {
    /**
     * Pulls product group from external integration service.
     *
     * @param ingestPullRequest ProductIngestPullRequest
     * @return Mono<ProductGroup>
     */
    Mono<ProductGroup> pullProductGroup(ProductIngestPullRequest ingestPullRequest);
}
