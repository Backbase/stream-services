package com.backbase.stream.configuration;

import com.backbase.dbs.transaction.api.service.v2.TransactionPresentationServiceApi;
import com.backbase.stream.TransactionService;
import com.backbase.stream.TransactionServiceImpl;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionTaskExecutor;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.repository.TransactionUnitOfWorkRepository;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(TransactionWorkerConfigurationProperties.class)
@AllArgsConstructor
@Configuration
public class TransactionServiceConfiguration {

    @Bean
    public TransactionTaskExecutor transactionTaskExecutor(
        TransactionPresentationServiceApi transactionsApi) {
        return new TransactionTaskExecutor(transactionsApi);
    }

    @Bean
    public TransactionUnitOfWorkExecutor transactionUnitOfWorkExecutor(
        TransactionTaskExecutor transactionTaskExecutor,
        TransactionUnitOfWorkRepository transactionUnitOfWorkRepository,
        TransactionWorkerConfigurationProperties transactionWorkerConfigurationProperties) {

        return new TransactionUnitOfWorkExecutor(
            transactionUnitOfWorkRepository,
            transactionTaskExecutor,
            transactionWorkerConfigurationProperties);
    }

    @Bean
    @ConditionalOnProperty(
        name = "backbase.stream.persistence",
        havingValue = "memory",
        matchIfMissing = true)
    public TransactionUnitOfWorkRepository transactionUnitOfWorkRepository() {
        return new InMemoryTransactionUnitOfWorkRepository();
    }

    @Bean
    public TransactionService transactionService(
        TransactionUnitOfWorkExecutor transactionTaskExecutor,
        TransactionPresentationServiceApi transactionsApi) {
        return new TransactionServiceImpl(transactionsApi, transactionTaskExecutor);
    }

    public static class InMemoryTransactionUnitOfWorkRepository
        extends InMemoryReactiveUnitOfWorkRepository<TransactionTask>
        implements TransactionUnitOfWorkRepository {

    }
}
