package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPushIngestionRequest;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.backbase.stream.legalentity.model.LegalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalEntityControllerTest {
    @Mock
    LegalEntityMapper mapper;

    @Mock
    LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    LegalEntityConfigurationProperties configProperties;

    @Mock
    ProductCompositionApi productCompositionApi;

    LegalEntityController controller;


    @BeforeEach
    void setUp() {
        controller = new LegalEntityController(
                legalEntityIngestionService,
                mapper,
                configProperties,
                productCompositionApi
        );
    }


    void testPullIngestion_Success() {
        Mono<LegalEntityPullIngestionRequest> requestMono = Mono.just(
                new LegalEntityPullIngestionRequest().withLegalEntityExternalId("externalId"));

        when(legalEntityIngestionService.ingestPull(any())).thenReturn(
                Mono.just(LegalEntityResponse.builder()
                        .legalEntity(new LegalEntity())
                        .build()));

        controller.pullLegalEntity(requestMono, null).block();
        verify(legalEntityIngestionService).ingestPull(any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<LegalEntityPushIngestionRequest> requestMono = Mono.just(
                new LegalEntityPushIngestionRequest().withLegalEntity(
                        new com.backbase.stream.compositions.legalentity.api.model.LegalEntity()));

        when(legalEntityIngestionService.ingestPush(any())).thenReturn(
                Mono.just(LegalEntityResponse.builder()
                        .legalEntity(new LegalEntity())
                        .build()));

        controller.pushLegalEntity(requestMono, null).block();
        verify(legalEntityIngestionService).ingestPush(any());
    }
}
