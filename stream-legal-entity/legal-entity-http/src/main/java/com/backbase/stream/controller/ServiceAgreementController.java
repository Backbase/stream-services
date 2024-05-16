package com.backbase.stream.controller;

import com.backbase.stream.UpdatedServiceAgreementSaga;
import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.UpdatedServiceAgreementUnitOfWorkExecutor;
import com.backbase.stream.legalentity.ServiceAgreementApi;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreement;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreementResponse;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@Slf4j
public class ServiceAgreementController extends BaseAsyncController implements ServiceAgreementApi {

    private final UpdatedServiceAgreementSaga updatedServiceAgreementService;
    private final UpdatedServiceAgreementUnitOfWorkExecutor updatedServiceAgreementUnitOfWorkExecutor;

    @Override
    public Mono<ResponseEntity<UpdatedServiceAgreementResponse>> getUpdateServiceAgreementUnitOfWork(
        String unitOfWorkId, ServerWebExchange exchange) {
        return updatedServiceAgreementUnitOfWorkExecutor.retrieve(unitOfWorkId)
            .map(unitOfWorkMapper::convertToUpdatedServiceAgreementResponse)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<UpdatedServiceAgreement>>> updateServiceAgreement(
        Flux<UpdatedServiceAgreement> updatedServiceAgreement, ServerWebExchange exchange) {
        Flux<UpdatedServiceAgreement> flux = updatedServiceAgreement
            .map(UpdatedServiceAgreementTask::new)
            .flatMap(updatedServiceAgreementService::executeTask)
            .map(UpdatedServiceAgreementTask::getData)
            .doOnNext(actual -> log.info("Finished Ingestion of Service Agreement: {}", actual.getExternalId()));

        return Mono.just(ResponseEntity.ok(flux));
    }

    @Override
    public Mono<ResponseEntity<Flux<UpdatedServiceAgreementResponse>>> updateServiceAgreementAsync(
        Flux<UpdatedServiceAgreement> updatedServiceAgreement,
        ServerWebExchange exchange) {
        Flux<UpdatedServiceAgreementResponse> map = updatedServiceAgreement.bufferTimeout(10, Duration.ofMillis(100))
            .map(this::createServiceAgreementUnitOfWork)
            .flatMap(updatedServiceAgreementUnitOfWorkExecutor::register)
            .map(unitOfWorkMapper::convertToUpdatedServiceAgreementResponse);
        return Mono.just(ResponseEntity.ok(map));
    }


}
