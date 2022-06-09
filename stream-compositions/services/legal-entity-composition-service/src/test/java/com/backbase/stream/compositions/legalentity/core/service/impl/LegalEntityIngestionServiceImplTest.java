package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityPostIngestionService;
import com.backbase.stream.compositions.legalentity.integration.client.model.LegalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestionServiceImplTest {
    private LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    private LegalEntityIntegrationService legalEntityIntegrationService;

    @Mock
    LegalEntityMapper mapper;

    @Mock
    Validator validator;

    @Mock
    LegalEntitySaga legalEntitySaga;

    @Mock
    LegalEntityPostIngestionService legalEntityPostIngestionService;

    @BeforeEach
    void setUp() {
        legalEntityIngestionService = new LegalEntityIngestionServiceImpl(
                legalEntitySaga,
                legalEntityIntegrationService,
                validator,
                legalEntityPostIngestionService);
    }

    void ingestionInPullMode_Success() {
        LegalEntityPullRequest legalEntityIngestPullRequest = LegalEntityPullRequest.builder()
                .legalEntityExternalId("externalId")
                .build();
        LegalEntity legalEntity = new LegalEntity().withName("legalEntityName");
        LegalEntityResponse res = new LegalEntityResponse(Boolean.TRUE,
                new com.backbase.stream.legalentity.model.LegalEntity().name("legalEntityName"), null);
        when(legalEntityIntegrationService.pullLegalEntity(legalEntityIngestPullRequest))
                .thenReturn(Mono.just(res));

        when(mapper.mapIntegrationToStream(legalEntity))
                .thenReturn(new com.backbase.stream.legalentity.model.LegalEntity().name(legalEntity.getName()));

        LegalEntityTask legalEntityTask = new LegalEntityTask();
        legalEntityTask.setLegalEntity(new com.backbase.stream.legalentity.model.LegalEntity().name("legalEntityName"));

        when(legalEntitySaga.executeTask(any()))
                .thenReturn(Mono.just(legalEntityTask));

        Mono<LegalEntityResponse> legalEntityIngestResponseMono = legalEntityIngestionService.ingestPull(legalEntityIngestPullRequest);
        assertNotNull(legalEntityIngestResponseMono.block());
        assertEquals("legalEntityName", legalEntityIngestResponseMono.block().getLegalEntity().getName());
    }

    void ingestionInPushMode_Unsupported() {
        LegalEntityPushRequest request = LegalEntityPushRequest.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> {
            legalEntityIngestionService.ingestPush(request);
        });
    }
}
