package com.backbase.stream.compositions.legalentity.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPushIngestionRequest;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.legalentity.model.LegalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LegalEntityControllerTest {

    LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);

    @Mock
    LegalEntityIngestionService legalEntityIngestionService;

    LegalEntityController controller;

    @BeforeEach
    void setUp() {
        controller = new LegalEntityController(legalEntityIngestionService, mapper);
    }

    @Test
    void testPullIngestion_Success() {
        Mono<LegalEntityPullIngestionRequest> requestMono =
            Mono.just(new LegalEntityPullIngestionRequest().withLegalEntityExternalId("externalId"));

        when(legalEntityIngestionService.ingestPull(any()))
            .thenReturn(
                Mono.just(LegalEntityResponse.builder().legalEntity(new LegalEntity()).build()));

        controller.pullLegalEntity(requestMono, null).block();
        verify(legalEntityIngestionService).ingestPull(any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<LegalEntityPushIngestionRequest> requestMono =
            Mono.just(
                new LegalEntityPushIngestionRequest()
                    .withLegalEntity(
                        new com.backbase.stream.compositions.legalentity.api.model.LegalEntity()));

        when(legalEntityIngestionService.ingestPush(any()))
            .thenReturn(
                Mono.just(LegalEntityResponse.builder().legalEntity(new LegalEntity()).build()));

        controller.pushLegalEntity(requestMono, null).block();
        verify(legalEntityIngestionService).ingestPush(any());
    }
}
