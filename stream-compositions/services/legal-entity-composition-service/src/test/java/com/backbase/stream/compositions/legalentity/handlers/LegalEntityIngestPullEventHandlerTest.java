package com.backbase.stream.compositions.legalentity.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.legalentity.model.LegalEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestPullEventHandlerTest {

    @Mock LegalEntityIngestionService legalEntityIngestionService;

    @Mock LegalEntityMapper mapper;

    @Mock EventBus eventBus;

    LegalEntityConfigurationProperties configProperties = new LegalEntityConfigurationProperties();

    LegalEntityPullEventHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new LegalEntityPullEventHandler(
                        configProperties, legalEntityIngestionService, mapper, eventBus);

        configProperties.setEvents(new LegalEntityConfigurationProperties.Events());
    }

    @Test
    void testHandleEvent_Completed() {
        LegalEntityResponse res =
                LegalEntityResponse.builder()
                        .legalEntity(new LegalEntity().name("Legal Entity"))
                        .build();
        Mono<LegalEntityResponse> responseMono = Mono.just(res);

        when(legalEntityIngestionService.ingestPull(any())).thenReturn(responseMono);

        LegalEntityPullRequest legalEntityPullRequest = new LegalEntityPullRequest();
        legalEntityPullRequest.setLegalEntityExternalId("externalLegalId");

        when(mapper.mapPullRequestEventToStream(any())).thenReturn(new LegalEntityPullRequest());

        configProperties.getEvents().setEnableCompleted(Boolean.TRUE);

        EnvelopedEvent<LegalEntityPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(
                new LegalEntityPullEvent().withLegalEntityExternalId("externalLegalId"));

        handler.handle(envelopedEvent);
        Mockito.verify(legalEntityIngestionService, Mockito.times(1)).ingestPull(any());
    }

    @Test
    void testHandleEvent_Failed() {
        LegalEntityPullRequest legalEntityPullRequest = new LegalEntityPullRequest();
        legalEntityPullRequest.setLegalEntityExternalId("externalLegalId");
        when(mapper.mapPullRequestEventToStream(any())).thenReturn(legalEntityPullRequest);

        when(legalEntityIngestionService.ingestPull(any()))
                .thenReturn(Mono.error(new RuntimeException("test error")));
        configProperties.getEvents().setEnableFailed(Boolean.TRUE);

        EnvelopedEvent<LegalEntityPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(
                new LegalEntityPullEvent().withLegalEntityExternalId("externalLegalId"));

        handler.handle(envelopedEvent);
        StepVerifier.create(legalEntityIngestionService.ingestPull(any())).expectError().verify();
    }
}
