package com.backbase.stream.controller;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.LegalEntityUnitOfWorkExecutor;
import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.UpdatedServiceAgreementUnitOfWorkExecutor;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.legalentity.LegalEntityApi;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityResponse;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreement;
import com.backbase.stream.mapper.UnitOfWorkMapper;
import com.backbase.stream.worker.model.UnitOfWork;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LegalEntityController extends BaseAsyncController implements LegalEntityApi {

    private final LegalEntitySaga legalEntitySaga;
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfiguration;
    private final LegalEntityUnitOfWorkExecutor legalEntityUnitOfWorkExecutor;

    @Override
    public Mono<ResponseEntity<Flux<LegalEntity>>> createLegalEntity(Flux<LegalEntity> legalEntity,
                                                                     ServerWebExchange exchange) {
        Flux<LegalEntity> flux = legalEntity
            .map(LegalEntityTask::new)
            .flatMap(legalEntitySaga::executeTask, legalEntitySagaConfiguration.getTaskExecutors())
            .map(LegalEntityTask::getData)
            .doOnNext(actual -> log.info("Finished Ingestion of Legal Entity: {}", actual.getExternalId()));

        return Mono.just(ResponseEntity.ok(flux));
    }

    @Override
    public Mono<ResponseEntity<LegalEntityResponse>> getUnitOfWork(String unitOfWorkId, ServerWebExchange exchange) {
        return legalEntityUnitOfWorkExecutor.retrieve(unitOfWorkId)
            .map(unitOfWorkMapper::convertToLegalEntityResponse)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalEntityResponse>>> processLegalEntitiesAsync(Flux<LegalEntity> legalEntity, ServerWebExchange exchange) {
        Flux<LegalEntityResponse> map = legalEntity.bufferTimeout(10, Duration.ofMillis(100))
            .map(this::createUnitOfWork)
            .flatMap(legalEntityUnitOfWorkExecutor::register)
            .map(unitOfWorkMapper::convertToLegalEntityResponse);
        return  Mono.just(ResponseEntity.ok(map));
    }

}
