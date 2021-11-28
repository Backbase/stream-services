package com.backbase.stream.compositions.legalentity;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPullResponse;
import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPushRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.worker.model.StreamTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIngestionService {
    private final LegalEntityConfiguration configuration;
    private final LegalEntityEventEmitter eventEmitter;
    private final LegalEntitySaga legalEntitySaga;
    private final LegalEntityIntegrationService legalEntityIntegrationService;

    public LegalEntityIngestPullResponse ingest(LegalEntityIngestPullRequest ingestPullRequest) {
        LegalEntity legalEntity = legalEntityIntegrationService.retrieveLegalEntity(ingestPullRequest);
        LegalEntity createdLegalEntity = sendLegalEntityToDbs(legalEntity);

        return LegalEntityIngestPullResponse.builder()
                .legalEntity(createdLegalEntity)
                .build();
    }

    public LegalEntityIngestPullResponse ingest(LegalEntityIngestPushRequest ingestPushRequest) {
        LegalEntity createdLegalEntity = sendLegalEntityToDbs(ingestPushRequest.getLegalEntity());

        return LegalEntityIngestPullResponse.builder()
                .legalEntity(createdLegalEntity)
                .build();
    }

    private LegalEntity sendLegalEntityToDbs(LegalEntity legalEntity) {
        LegalEntityTask legalEntityTask = Mono.just(legalEntity)
                .map(LegalEntityTask::new)
                .flatMap(legalEntitySaga::executeTask)
                .doOnNext(StreamTask::logSummary)
                .doOnError(this::emitFailedEvent)
                .doOnSuccess(this::emitCompletedEvent)
                .block();

        return legalEntityTask.getData();
    }

    private void emitFailedEvent(Throwable a) {
        eventEmitter.emitFailedEvent();
    }

    private void emitCompletedEvent(LegalEntityTask legalEntityTask) {
        eventEmitter.emitCompletedEvent();
    }
}
