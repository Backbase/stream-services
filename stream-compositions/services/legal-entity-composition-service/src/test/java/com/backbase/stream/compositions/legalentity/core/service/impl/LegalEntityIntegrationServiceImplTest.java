package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.compositions.integration.legalentity.api.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityListResponse;
import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
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
        LegalEntity legalEntity1 = new LegalEntity().name("Legal Entity 1");
        LegalEntity legalEntity2 = new LegalEntity().name("Legal Entity 2");

        GetLegalEntityListResponse getLegalEntityListResponse = new GetLegalEntityListResponse()
                .addLegalEntitiesItem(legalEntity1)
                .addLegalEntitiesItem(legalEntity2);

        when(legalEntityIntegrationApi.getLegalEntities(any()))
                .thenReturn(Mono.just(getLegalEntityListResponse));

        GetLegalEntityListResponse response = legalEntityIntegrationService.retrieveLegalEntities(
                LegalEntityIngestPullRequest.builder().legalEntityExternalId("externalId").build()).block();

        assertEquals(2, response.getLegalEntities().size());
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(legalEntityIntegrationApi.getLegalEntities(any()))
                .thenReturn(Mono.empty());

        GetLegalEntityListResponse response = legalEntityIntegrationService.retrieveLegalEntities(
                LegalEntityIngestPullRequest.builder().legalEntityExternalId("externalId").build()).block();

        assertNull(response);
    }
}

