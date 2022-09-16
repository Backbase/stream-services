package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import reactor.core.publisher.Mono;

public interface ProductIntegrationService {
    /**
     * Pulls product group from external integration service.
     *
     * @param ingestPullRequest ProductIngestPullRequest
     * @return Mono<ProductGroup>
     */
    Mono<ProductIngestResponse> pullProductGroup(ProductIngestPullRequest ingestPullRequest);
}
