package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIngestionService {
    private final LegalEntityMapper mapper;
    private final LegalEntitySaga legalEntitySaga;
    private final LegalEntityIntegrationService legalEntityIntegrationService;

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    public Mono<LegalEntityIngestResponse> ingestPull(LegalEntityIngestPullRequest ingestPullRequest) {
        return ingest(legalEntityIntegrationService
                .retrieveLegalEntities(ingestPullRequest)
                .map(mapper::mapIntegrationToStream));
    }

    /**
     * Ingests legal entity in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return LegalEntityIngestResponse
     */
    public Mono<LegalEntityIngestResponse> ingestPush(LegalEntityIngestPushRequest ingestPushRequest) {
        return ingest(Flux.fromIterable(ingestPushRequest.getLegalEntities()));
    }

    /**
     * Ingests legal entities to DBS.
     *
     * @param legalEnities List of legal entities
     * @return Ingested legal entities
     */
    private Mono<LegalEntityIngestResponse> ingest(Flux<LegalEntity> legalEnities) {
        return legalEnities
                .flatMap(this::sendLegalEntityToDbs)
                .collectList()
                .doOnSuccess(this::handleSuccess)
                .doOnError(this::handleError)
                .map(this::buildResponse);
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
