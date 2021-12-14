package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import reactor.core.publisher.Mono;

public interface LegalEntityIngestionService {
    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    Mono<LegalEntityIngestResponse> ingestPull(Mono<LegalEntityIngestPullRequest> ingestPullRequest);

    /**
     * Ingests legal entity in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return LegalEntityIngestResponse
     */
    Mono<LegalEntityIngestResponse> ingestPush(Mono<LegalEntityIngestPushRequest> ingestPushRequest);
}
