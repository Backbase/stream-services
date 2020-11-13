package com.backbase.stream.controller;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.api.LegalEntityApi;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class LegalEntityController implements LegalEntityApi {

    private final LegalEntitySaga legalEntityService;

    public LegalEntityController(LegalEntitySaga legalEntityService) {
        this.legalEntityService = legalEntityService;
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalEntity>>> createLegalEntity(Flux<LegalEntity> legalEntity,
                                                                     ServerWebExchange exchange) {
        Flux<LegalEntity> flux = legalEntity
            .map(LegalEntityTask::new)
            .flatMap(legalEntityService::executeTask)
            .map(LegalEntityTask::getData)
            .doOnNext(actual -> log.info("Finished Ingestion of Legal Entity: {}", actual.getExternalId()));

        return Mono.just(ResponseEntity.ok(flux));
    }
}
