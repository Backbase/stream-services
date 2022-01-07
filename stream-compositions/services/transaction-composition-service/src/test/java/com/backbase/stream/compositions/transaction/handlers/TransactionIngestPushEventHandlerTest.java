package com.backbase.stream.compositions.transaction.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsIngestPushEvent;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionIngestPushEventHandlerTest {
    @Mock
    private TransactionIngestionService transactionIngestionService;

    @Mock
    TransactionMapper mapper;

    @Test
    void testHandleEvent_Completed() {
        List<TransactionsPostResponseBody> transactions = new ArrayList<>();

        Mono<TransactionIngestResponse> responseMono = Mono.just(
                TransactionIngestResponse
                        .builder().transactions(transactions).build());

        lenient().when(transactionIngestionService.ingestPush(any())).thenReturn(responseMono);

        TransactionIngestPushEventHandler handler = new TransactionIngestPushEventHandler(
                transactionIngestionService,
                mapper);

        EnvelopedEvent<TransactionsIngestPushEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new TransactionsIngestPushEvent());

        handler.handle(envelopedEvent);
        verify(transactionIngestionService).ingestPush(any());
    }
}
