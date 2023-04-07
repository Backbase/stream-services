package com.backbase.stream.controller;

import com.backbase.stream.UpdatedServiceAgreementSaga;
import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.legalentity.api.ServiceAgreementApi;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreement;

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
public class ServiceAgreementController implements ServiceAgreementApi {

    private final UpdatedServiceAgreementSaga updatedServiceAgreementService;

    @Override
    public Mono<ResponseEntity<Flux<UpdatedServiceAgreement>>> updateServiceAgreement(
            Flux<UpdatedServiceAgreement> updatedServiceAgreement, ServerWebExchange exchange) {
        Flux<UpdatedServiceAgreement> flux =
                updatedServiceAgreement
                        .map(UpdatedServiceAgreementTask::new)
                        .flatMap(updatedServiceAgreementService::executeTask)
                        .map(UpdatedServiceAgreementTask::getData)
                        .doOnNext(
                                actual ->
                                        log.info(
                                                "Finished Ingestion of Service Agreement: {}",
                                                actual.getExternalId()));

        return Mono.just(ResponseEntity.ok(flux));
    }
}
