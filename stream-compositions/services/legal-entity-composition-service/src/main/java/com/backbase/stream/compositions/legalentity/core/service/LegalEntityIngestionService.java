package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
public class LegalEntityIngestionService {
    private final LegalEntityMapper mapper;
    private final LegalEntitySaga legalEntitySaga;
    private final LegalEntityIntegrationService legalEntityIntegrationService;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

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
     * Ingests root legal entity.
     *
     * @return LegalEntityIngestResponse
     */
    public Mono<LegalEntityIngestResponse> ingestRoot() {
        LegalEntity rootLegalEntity = bootstrapConfigurationProperties.getLegalEntity();
        log.info("Bootstrapping root legal entity: {}.", rootLegalEntity.getName());
        return ingest(Flux.just(rootLegalEntity))
                .doOnError(ex -> log.warn("Failed to load root legal entity", ex))
                .doOnSuccess(result -> log.info("Root legal entity bootstrapping complete. Internal ID: {}.", result));
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
                .map(item -> item.getData());
    }

    private LegalEntityIngestResponse buildResponse(List<LegalEntity> legalEnityList) {
        return LegalEntityIngestResponse.builder()
                .legalEntities(legalEnityList)
                .build();
    }

    private void handleSuccess(List<LegalEntity> legalEntities) {
        log.error("Legal entities ingestion completed (count: {})", legalEntities.size());
        if (log.isDebugEnabled()) {
            log.debug("Ingested legal entities: {}", legalEntities);
        }
    }

    private void handleError(Throwable ex) {
        log.error("Legal entity ingestion failed", ex);
    }
}
