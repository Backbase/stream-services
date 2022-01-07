package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIngestionServiceImpl implements LegalEntityIngestionService {
    private final LegalEntityMapper mapper;
    private final LegalEntitySaga legalEntitySaga;
    private final LegalEntityIntegrationService legalEntityIntegrationService;

    /**
     * {@inheritDoc}
     */
    public Mono<LegalEntityIngestResponse> ingestPull(Mono<LegalEntityIngestPullRequest> ingestPullRequest) {
        return ingestPullRequest
                .map(this::pullLegalEntity)
                .flatMap(this::sendToDbs)
                .doOnSuccess(this::handleSuccess)
                .map(this::buildResponse);
    }

    /**
     * {@inheritDoc}
     */
    public Mono<LegalEntityIngestResponse> ingestPush(Mono<LegalEntityIngestPushRequest> ingestPushRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pulls and remaps legal entity from integration service.
     *
     * @param request LegalEntityIngestPullRequest
     * @return LegalEntity
     */
    private Mono<LegalEntity> pullLegalEntity(LegalEntityIngestPullRequest request) {
        return legalEntityIntegrationService
                .pullLegalEntity(request)
                .map(mapper::mapIntegrationToStream);
    }

    /**
     * Sends product group to DBS.
     *
     * @param legalEntity LegalEntity
     * @return LegalEntity
     */
    private Mono<LegalEntity> sendToDbs(Mono<LegalEntity> legalEntity) {
        return legalEntity
                .map(LegalEntityTask::new)
                .flatMap(legalEntitySaga::executeTask)
                .map(LegalEntityTask::getData );
    }

    private LegalEntityIngestResponse buildResponse(LegalEntity legalEnity) {
        return LegalEntityIngestResponse.builder()
                .legalEntity(legalEnity)
                .build();
    }

    private void handleSuccess(LegalEntity legalEntity) {
        log.info("Legal entities ingestion completed.");
        if (log.isDebugEnabled()) {
            log.debug("Ingested legal entity: {}", legalEntity);
        }
    }
}
