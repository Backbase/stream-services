package com.backbase.stream.transaction.sink;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.TransactionService;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Input channel for ingesting transactions.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class TransactionSinkConfiguration {

    private final TransactionService transactionService;

    public Function<Flux<Message<TransactionsPostRequestBody>>, Mono<Message<Void>>> transactionConsumer() {
        return messageFlux -> {
            Flux<TransactionsPostRequestBody> payload = messageFlux.map(Message::getPayload);
            Flux<UnitOfWork<TransactionTask>> processTransactions = transactionService.processTransactions(payload);
            return processTransactions
                .doOnNext(transactionTaskUnitOfWork -> log.info("Processed unit of work: {}", transactionTaskUnitOfWork))
                .then().map(v -> MessageBuilder.withPayload(v).build());
        };
    }

}
