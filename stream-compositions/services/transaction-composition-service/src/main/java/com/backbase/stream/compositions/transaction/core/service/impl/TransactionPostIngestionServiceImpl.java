package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.compositions.events.egress.event.spec.v1.TransactionsCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.TransactionsFailedEvent;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.service.TransactionPostIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionPostIngestionServiceImpl implements TransactionPostIngestionService {
    private final EventBus eventBus;

    private final TransactionConfigurationProperties transactionConfigurationProperties;

    @Override
    public void handleSuccess(List<TransactionsPostResponseBody> res) {
        log.info("Transaction ingestion completed successfully.");
        if (Boolean.TRUE.equals(transactionConfigurationProperties.getEvents().getEnableCompleted())) {
            TransactionsCompletedEvent event = new TransactionsCompletedEvent()
                    .withTransactionIds(res.stream().map(TransactionsPostResponseBody::getId).collect(Collectors.toList()));
            EnvelopedEvent<TransactionsCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }

        log.debug("Ingested Transactions: {}", res);
    }

    @Override
    public Mono<List<TransactionsPostResponseBody>> handleFailure(Throwable error) {
        log.error("Transaction ingestion failed. {}", error.getMessage());
        if (Boolean.TRUE.equals(transactionConfigurationProperties.getEvents().getEnableFailed())) {
            TransactionsFailedEvent event = new TransactionsFailedEvent()
                    .withMessage(error.getMessage());
            EnvelopedEvent<TransactionsFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
        return Mono.empty();
    }
}
