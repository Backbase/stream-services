package com.backbase.stream.service;

import com.backbase.dbs.accesscontrol.api.service.v2.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v2.LegalEntityApi;
import com.backbase.dbs.accesscontrol.api.service.v2.model.BatchResponseItem;
import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntityItemBase;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.mapper.LegalEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalEntityServiceTest {

    private LegalEntityService subject;

    @Mock
    private LegalEntitiesApi legalEntitiesApi;
    @Mock
    private LegalEntityApi legalEntityApi;

    private LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);

    @BeforeEach
    void setup() {
        subject = new LegalEntityService(legalEntitiesApi, legalEntityApi);
    }

    @Test
    void updateLegalEntity_success() {
        final String externalId = "someExternalId";
        final String internalId = "someInternalId";
        final String oldName = "oldName";
        final String newName = "newName";
        LegalEntity legalEntity = new LegalEntity().externalId(externalId).internalId(internalId).name(oldName);
        LegalEntity legalEntityUpdated = new LegalEntity().externalId(externalId).internalId(internalId).name(newName);

        LegalEntityItemBase leItemBase = new LegalEntityItemBase().id(internalId).externalId(externalId).name(newName);

        BatchResponseItem batchResponseItem = new BatchResponseItem().status(BatchResponseItem.StatusEnum.HTTP_STATUS_OK);

        when(legalEntitiesApi.putLegalEntities(anyList())).thenReturn(Flux.just(batchResponseItem));
        when(legalEntitiesApi.getLegalEntityByExternalId(externalId)).thenReturn(Mono.just(leItemBase));

        Mono<LegalEntity> result = subject.putLegalEntity(legalEntity);

        StepVerifier.create(result)
                .assertNext(assertEqualsTo(legalEntityUpdated))
                .verifyComplete();
    }

    @Test
    void updateLegalEntity_fail() {
        final String externalId = "someExternalId";
        final String internalId = "someInternalId";
        final String oldName = "oldName";
        final String newName = "newName";
        LegalEntity legalEntity = new LegalEntity().externalId(externalId).internalId(internalId).name(oldName);
        LegalEntity legalEntityUpdated = new LegalEntity().externalId(externalId).internalId(internalId).name(newName);

        LegalEntityItemBase leItemBase = new LegalEntityItemBase().id(internalId).externalId(externalId).name(newName);

        BatchResponseItem batchResponseItem = new BatchResponseItem().status(BatchResponseItem.StatusEnum.HTTP_STATUS_OK);

        when(legalEntitiesApi.putLegalEntities(anyList())).thenReturn(Flux.just(batchResponseItem));
        when(legalEntitiesApi.getLegalEntityByExternalId(externalId)).thenReturn(Mono.just(leItemBase));

        Mono<LegalEntity> result = subject.putLegalEntity(legalEntity);

        StepVerifier.create(result)
                .assertNext(assertEqualsTo(legalEntityUpdated))
                .verifyComplete();
    }
}
