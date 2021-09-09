package com.backbase.stream.transaction.sink;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.TransactionService;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.config.JsonMessageConverter;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
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

    @Bean
    public Function<Flux<Message<TransactionsPostRequestBody>>, Mono<Message<Void>>> transactionConsumer() {
        return messageFlux -> {
            Flux<TransactionsPostRequestBody> payload = messageFlux.map(Message::getPayload);
            Flux<UnitOfWork<TransactionTask>> processTransactions = transactionService.processTransactions(payload);
            return processTransactions
                .onErrorResume(StreamTaskException.class, throwable -> {
                    log.error("Failed to ingest transactions: {}", throwable.getMessage());
                    return Mono.empty();
                })
                .then()
                .map(MessageBuilder::withPayload)
                .map(MessageBuilder::build);
        };
    }

    @Bean
    public MessageConverter customMessageConverter(JsonMapper jsonMapper) {
        return new JsonMessageConverter(jsonMapper, new MimeType("text", "plain"));
    }

}
