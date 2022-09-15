package com.backbase.stream.compositions.paymentorders.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.paymentorders.core.config.PaymentOrderConfigurationProperties;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderPostIngestionService;
import com.backbase.stream.model.PaymentOrderIngestContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentOrderPostIngestionServiceImpl implements PaymentOrderPostIngestionService {
    private final EventBus eventBus;

    private final PaymentOrderConfigurationProperties paymentOrderConfigurationProperties;

    @Override
    public void handleSuccess(PaymentOrderIngestContext response) {
        log.info("Payment Order ingestion completed successfully.");
        if (Boolean.TRUE.equals(paymentOrderConfigurationProperties.getEvents().getEnableCompleted())) {
            //todo log event
//            TransactionsCompletedEvent event = new TransactionsCompletedEvent()
//                    .withTransactionIds(res.stream().map(TransactionsPostResponseBody::getId).collect(Collectors.toList()));
//            EnvelopedEvent<TransactionsCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
//            envelopedEvent.setEvent(event);
//            eventBus.emitEvent(envelopedEvent);
        }

        log.debug("Ingested Payments: {}", response);
    }

    @Override
    public Mono<PaymentOrderIngestContext> handleFailure(Throwable error) { //Mono<List<PaymentOrderPostResponse>> handleFailure(Throwable error) {
        log.error("Payment Order ingestion failed. {}", error.getMessage());
        //todo handle event
//        if (Boolean.TRUE.equals(transactionConfigurationProperties.getEvents().getEnableFailed())) {
//            TransactionsFailedEvent event = new TransactionsFailedEvent()
//                    .withMessage(error.getMessage());
//            EnvelopedEvent<TransactionsFailedEvent> envelopedEvent = new EnvelopedEvent<>();
//            envelopedEvent.setEvent(event);
//            eventBus.emitEvent(envelopedEvent);
//        }
        return Mono.empty();
    }
}
