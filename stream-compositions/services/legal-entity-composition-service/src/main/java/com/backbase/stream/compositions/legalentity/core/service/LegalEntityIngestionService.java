package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import reactor.core.publisher.Mono;

public interface LegalEntityIngestionService {
    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    Mono<LegalEntityResponse> ingestPull(Mono<LegalEntityPullRequest> ingestPullRequest);

    /**
     * Ingests legal entity in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return LegalEntityIngestResponse
     */
    Mono<LegalEntityResponse> ingestPush(Mono<LegalEntityPushRequest> ingestPushRequest);
}
