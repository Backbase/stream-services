package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.integration.legalentity.api.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.service.impl.LegalEntityIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalEntityIntegrationServiceImplTest {
    @Mock
    private LegalEntityIntegrationApi legalEntityIntegrationApi;

    private LegalEntityIntegrationServiceImpl legalEntityIntegrationService;

    @BeforeEach
    void setUp() {
        legalEntityIntegrationService = new LegalEntityIntegrationServiceImpl(legalEntityIntegrationApi);
    }

    @Test
    void callIntegrationService_LegalEntitiesFound() throws UnsupportedOperationException {
        LegalEntity legalEntity1 = new LegalEntity().name("Legal Enity 1");
        LegalEntity legalEntity2 = new LegalEntity().name("Legal Enity 2");
        when(legalEntityIntegrationApi.getLegalEntities(any()))
                .thenReturn(Flux.just(legalEntity1, legalEntity2));

        Flux<LegalEntity> legalEntities = legalEntityIntegrationService.retrieveLegalEntities(
                LegalEntityIngestPullRequest.builder().legalEntityExternalId("externalId").build());

        assertEquals(2, legalEntities.collectList().block().size());
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(legalEntityIntegrationApi.getLegalEntities(any()))
                .thenReturn(Flux.just());

        Flux<LegalEntity> legalEntities = legalEntityIntegrationService.retrieveLegalEntities(
                LegalEntityIngestPullRequest.builder().legalEntityExternalId("externalId").build());

        assertEquals(0, legalEntities.collectList().block().size());
    }
}

