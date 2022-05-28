package com.backbase.stream.compositions.transaction.core.service;

import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.model.TransactionsPostRequestBody;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bootstrap.enabled", matchIfMissing = false)
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
public class TransactionsBootstrapTask implements ApplicationRunner {
    private final TransactionService transactionService;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;
    private final TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    @Override
    public void run(ApplicationArguments args) {
        List<TransactionsPostRequestBody> transactions= bootstrapConfigurationProperties.getTransactions();
        bootstrapTransactions(transactions).subscribe();
    }

    private Flux<UnitOfWork<TransactionTask>> bootstrapTransactions(List<TransactionsPostRequestBody> transactions) {
        if (null == transactions) {
            log.warn("Failed to load Transactions.");
            return Flux.empty();
        } else {
            log.info("Bootstrapping Transactions.");

            return transactionService.processTransactions(
                Flux.fromIterable(transactions).map(transactionMapper::mapCompositionToStream));
        }
    }
}
