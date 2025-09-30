package com.backbase.stream.service;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.utils.BatchResponseUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LegalEntityServiceTest {

    private LegalEntityService subject;

    @Mock
    private LegalEntityApi legalEntitiesApi;
    @Mock
    private com.backbase.accesscontrol.legalentity.api.integration.v3.LegalEntityApi legalEntityIntegrationApi;

    @BeforeEach
    void setup() {
        subject = new LegalEntityService(legalEntitiesApi, legalEntityIntegrationApi, new BatchResponseUtils());
    }

    @Test
    void updateLegalEntity_success() {
        final String externalId = "someExternalId";
        final String internalId = "someInternalId";
        final String oldName = "oldName";
        final String newName = "newName";
        LegalEntity legalEntity = new LegalEntity().externalId(externalId).internalId(internalId).name(oldName);
        LegalEntity legalEntityUpdated = new LegalEntity().externalId(externalId).internalId(internalId).name(newName);

        com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntity leItemBase = new com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntity().externalId(
            externalId).name(newName).id(internalId);

        when(legalEntitiesApi.updateLegalEntity(any(), any())).thenReturn(Mono.empty());
        when(legalEntityIntegrationApi.getLegalEntityByExternalId(externalId))
            .thenReturn(Mono.just(leItemBase));

        Mono<LegalEntity> result = subject.putLegalEntity(legalEntity);

        StepVerifier.create(result)
            .assertNext(assertEqualsTo(legalEntityUpdated))
            .verifyComplete();
    }

    @Test
    void shouldReturnSubEntities() {
        when(legalEntityIntegrationApi.getLegalEntityByExternalId("external-id"))
            .thenReturn(Mono.just(
                new com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntity().id("internal-id")));
        when(legalEntitiesApi.getLegalEntities("internal-id", "cursor", 10, null, null, null))
            .thenReturn(Mono.just(new com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntitiesList()
                .legalEntities(List.of(new com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityItem()
                    .id("internal-id")
                    .externalId("external-id"))
                ).nextPage("next-cursor")));
        subject.getSubEntities("external-id", "cursor", 10)
            .as(StepVerifier::create)
            .assertNext(
                assertEqualsTo(new com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntitiesList()
                    .legalEntities(
                        List.of(new com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityItem()
                            .id("internal-id")
                            .externalId("external-id"))
                    ).nextPage("next-cursor")))
            .verifyComplete();
    }
}
