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

import java.util.List;

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
                .flatMapMany(legalEntityIntegrationService::retrieveLegalEntities)
                .map(mapper::mapIntegrationToStream)
                .flatMap(this::sendLegalEntityToDbs)
                .collectList()
                .map(this::buildResponse);
    }

    /**
     * {@inheritDoc}
     */
    public Mono<LegalEntityIngestResponse> ingestPush(Mono<LegalEntityIngestPushRequest> ingestPushRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Ingests single legal entity to DBS.
     *
     * @param legalEntity Legal entity
     * @return Ingested legal entities
     */
    private Mono<LegalEntity> sendLegalEntityToDbs(LegalEntity legalEntity) {
        return Mono.just(legalEntity)
                .map(LegalEntityTask::new)
                .flatMap(legalEntitySaga::executeTask)
                .map(LegalEntityTask::getData);
    }

    private LegalEntityIngestResponse buildResponse(List<LegalEntity> legalEnityList) {
        return LegalEntityIngestResponse.builder()
                .legalEntities(legalEnityList)
                .build();
    }

    private void handleSuccess(List<LegalEntity> legalEntities) {
        log.info("Legal entities ingestion completed (count: {})", legalEntities.size());
        if (log.isDebugEnabled()) {
            log.debug("Ingested legal entities: {}", legalEntities);
        }
    }

    private void handleError(Throwable ex) {
        log.error("Legal entity ingestion failed", ex);
    }
}
