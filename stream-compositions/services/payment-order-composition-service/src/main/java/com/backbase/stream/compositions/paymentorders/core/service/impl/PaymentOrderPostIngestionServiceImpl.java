package com.backbase.stream.compositions.paymentorders.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.compositions.paymentorders.core.config.PaymentOrderConfigurationProperties;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderPostIngestionService;
import com.backbase.stream.model.PaymentOrderIngestContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentOrderPostIngestionServiceImpl implements PaymentOrderPostIngestionService {
    private final EventBus eventBus;

    private final PaymentOrderConfigurationProperties paymentOrderConfigurationProperties;

    //todo with payment context
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

//    @Override
//    public void handleSuccess(List<PaymentOrderPostResponse> response) {
//        log.info("Payment Order ingestion completed successfully.");
//        if (Boolean.TRUE.equals(paymentOrderConfigurationProperties.getEvents().getEnableCompleted())) {
//            //todo log event
////            TransactionsCompletedEvent event = new TransactionsCompletedEvent()
////                    .withTransactionIds(res.stream().map(TransactionsPostResponseBody::getId).collect(Collectors.toList()));
////            EnvelopedEvent<TransactionsCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
////            envelopedEvent.setEvent(event);
////            eventBus.emitEvent(envelopedEvent);
//        }
//
//        log.debug("Ingested Payments: {}", response);
//    }

    // todo with payment context
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

//    @Override
//    public Mono<List<PaymentOrderPostResponse>> handleFailure(Throwable error) {
//        System.out.println("PaymentOrderPostIngestionServiceImpl :: handleFailure :: " + error.getMessage());
//        log.error("Payment Order ingestion failed. {}", error.getMessage());
//        //todo handle event
////        if (Boolean.TRUE.equals(transactionConfigurationProperties.getEvents().getEnableFailed())) {
////            TransactionsFailedEvent event = new TransactionsFailedEvent()
////                    .withMessage(error.getMessage());
////            EnvelopedEvent<TransactionsFailedEvent> envelopedEvent = new EnvelopedEvent<>();
////            envelopedEvent.setEvent(event);
////            eventBus.emitEvent(envelopedEvent);
////        }
//        return Mono.empty();
//    }

//    @Override
//    public void handleSuccess(List<TransactionsPostResponseBody> res) {
//        log.info("Transaction ingestion completed successfully.");
//        if (Boolean.TRUE.equals(transactionConfigurationProperties.getEvents().getEnableCompleted())) {
//            TransactionsCompletedEvent event = new TransactionsCompletedEvent()
//                    .withTransactionIds(res.stream().map(TransactionsPostResponseBody::getId).collect(Collectors.toList()));
//            EnvelopedEvent<TransactionsCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
//            envelopedEvent.setEvent(event);
//            eventBus.emitEvent(envelopedEvent);
//        }
//
//        log.debug("Ingested Transactions: {}", res);
//    }
//
//    @Override
//    public Mono<List<TransactionsPostResponseBody>> handleFailure(Throwable error) {
//        log.error("Transaction ingestion failed. {}", error.getMessage());
//        if (Boolean.TRUE.equals(transactionConfigurationProperties.getEvents().getEnableFailed())) {
//            TransactionsFailedEvent event = new TransactionsFailedEvent()
//                    .withMessage(error.getMessage());
//            EnvelopedEvent<TransactionsFailedEvent> envelopedEvent = new EnvelopedEvent<>();
//            envelopedEvent.setEvent(event);
//            eventBus.emitEvent(envelopedEvent);
//        }
//        return Mono.empty();
//    }
}
