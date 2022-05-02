package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.compositions.legalentity.integration.client.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.legalentity.integration.client.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.integration.client.model.PullLegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        LegalEntity legalEntity1 = new LegalEntity().withName("Legal Entity 1");

        PullLegalEntityResponse getLegalEntityListResponse = new PullLegalEntityResponse().withLegalEntity(legalEntity1);

        when(legalEntityIntegrationApi.pullLegalEntity(any(), any()))
                .thenReturn(Mono.just(getLegalEntityListResponse));

        LegalEntity legalEntityResponse = legalEntityIntegrationService.pullLegalEntity(
                LegalEntityPullRequest.builder().legalEntityExternalId("externalId").build()).block();

        assertEquals("Legal Entity 1", legalEntityResponse.getName());
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(legalEntityIntegrationApi.pullLegalEntity(any(), any()))
                .thenReturn(Mono.empty());

        LegalEntity legalEntity = legalEntityIntegrationService.pullLegalEntity(
                LegalEntityPullRequest.builder().legalEntityExternalId("externalId").build()).block();

        assertNull(legalEntity);
    }
}

