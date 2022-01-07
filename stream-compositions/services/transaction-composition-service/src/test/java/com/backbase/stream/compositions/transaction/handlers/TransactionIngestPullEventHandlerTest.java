package com.backbase.stream.compositions.transaction.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsIngestPullEvent;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionIngestPullEventHandlerTest {
    @Mock
    private TransactionIngestionService transactionIngestionService;

    @Mock
    TransactionMapper mapper;

    @Mock
    EventBus eventBus;

    @Test
    void testHandleEvent_Completed() {
        List<TransactionsPostResponseBody> transactions = new ArrayList<>();

        Mono<TransactionIngestResponse> responseMono = Mono.just(
                TransactionIngestResponse
                        .builder().transactions(transactions).build());

        lenient().when(transactionIngestionService.ingestPull(any())).thenReturn(responseMono);
        TransactionConfigurationProperties properties = new TransactionConfigurationProperties();

        TransactionIngestPullEventHandler handler = new TransactionIngestPullEventHandler(
                properties,
                transactionIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<TransactionsIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new TransactionsIngestPullEvent());

        handler.handle(envelopedEvent);
        verify(transactionIngestionService).ingestPull(any());
    }

    @Test
    void testHandleEvent_Failed() {
        when(transactionIngestionService.ingestPull(any())).thenThrow(new RuntimeException());

        TransactionConfigurationProperties properties = new TransactionConfigurationProperties();

        TransactionIngestPullEventHandler handler = new TransactionIngestPullEventHandler(
                properties,
                transactionIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<TransactionsIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        TransactionsIngestPullEvent event = new TransactionsIngestPullEvent().withExternalArrangementIds("externalId");
        envelopedEvent.setEvent(event);

        assertThrows(RuntimeException.class, () -> {
            handler.handle(envelopedEvent);
        });
    }
}
