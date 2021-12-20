package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityListResponse;
import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapperImpl;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestionServiceImplTest {
    private LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    private LegalEntityIntegrationService legalEntityIntegrationService;

    @Mock
    LegalEntityMapperImpl mapper;

    @Mock
    LegalEntitySaga legalEntitySaga;

    @BeforeEach
    void setUp() {
        legalEntityIngestionService = new LegalEntityIngestionServiceImpl(
                mapper,
                legalEntitySaga,
                legalEntityIntegrationService);
    }

    @Test
    void ingestionInPullMode_Success() {
        Mono<LegalEntityIngestPullRequest> legalEntityIngestPullRequest = Mono.just(LegalEntityIngestPullRequest.builder()
                .legalEntityExternalId("externalId")
                .build());
        LegalEntity legalEntity = new LegalEntity().name("legalEntityName");
        GetLegalEntityListResponse getLegalEntityListResponse = new GetLegalEntityListResponse().addLegalEntitiesItem(legalEntity);
        when(legalEntityIntegrationService.retrieveLegalEntities(legalEntityIngestPullRequest.block()))
                .thenReturn(Mono.just(getLegalEntityListResponse));

        when(mapper.mapIntegrationToStream(legalEntity))
                .thenReturn(new com.backbase.stream.legalentity.model.LegalEntity().name(legalEntity.getName()));

        LegalEntityTask legalEntityTask = new LegalEntityTask();
        legalEntityTask.setLegalEntity(new com.backbase.stream.legalentity.model.LegalEntity().name("legalEntityName"));

        when(legalEntitySaga.executeTask(any()))
                .thenReturn(Mono.just(legalEntityTask));

        Mono<LegalEntityIngestResponse> legalEntityIngestResponseMono = legalEntityIngestionService.ingestPull(legalEntityIngestPullRequest);
        assertEquals(1, legalEntityIngestResponseMono.block().getLegalEntities().size());
        assertEquals("legalEntityName", legalEntityIngestResponseMono.block().getLegalEntities().get(0).getName());
    }

    @Test
    void ingestionInPushMode_Unsupported() {
        Mono<LegalEntityIngestPushRequest> request = Mono.just(LegalEntityIngestPushRequest.builder().build());
        assertThrows(UnsupportedOperationException.class, () -> {
            legalEntityIngestionService.ingestPush(request);
        });
    }
}
