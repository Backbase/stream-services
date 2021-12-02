package com.backbase.stream.compositions.legalentity.core;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.events.LegalEntityEventEmitter;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.worker.model.StreamTask;
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
    private final LegalEntityEventEmitter eventEmitter;
    private final LegalEntitySaga legalEntitySaga;
    private final LegalEntityIntegrationService legalEntityIntegrationService;

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    public LegalEntityIngestResponse ingestPull(LegalEntityIngestPullRequest ingestPullRequest) {
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
    public LegalEntityIngestResponse ingestPush(LegalEntityIngestPushRequest ingestPushRequest) {
        return ingest(Flux
                .fromIterable(ingestPushRequest.getLegalEntities()));
    }

    /**
     * Ingests legal entities to DBS.
     *
     * @param legalEnities List of legal entities
     * @return Ingested legal entities
     */
    private LegalEntityIngestResponse ingest(Flux<LegalEntity> legalEnities) {
        return legalEnities
                .map(this::sendLegalEntityToDbs)
                .collectList()
                .doOnSuccess(this::emitCompletedEvent)
                .doOnError(this::emitFailedEvent)
                .map(this::buildResponse)
                .block();
    }

    /**
     * Ingests single legal entity to DBS.
     *
     * @param legalEntity Legal entity
     * @return Ingested legal entities
     */
    private LegalEntity sendLegalEntityToDbs(LegalEntity legalEntity) {
        return Mono.just(legalEntity)
                .map(LegalEntityTask::new)
                .flatMap(legalEntitySaga::executeTask)
                .doOnNext(StreamTask::logSummary)
                .block()
                .getData();
    }

    private LegalEntityIngestResponse buildResponse(List<LegalEntity> legalEnityList) {
        return LegalEntityIngestResponse.builder()
                .legalEntities(legalEnityList)
                .build();
    }

    private void emitFailedEvent(Throwable a) {
        // TODO: implement
        eventEmitter.emitFailedEvent();
    }

    private void emitCompletedEvent(List<LegalEntity> legalEnityList) {
        // TODO: implement
        eventEmitter.emitCompletedEvent();
    }
}
