package com.backbase.stream.configuration;

import com.backbase.dbs.transaction.presentation.service.ApiClient;
import com.backbase.dbs.transaction.presentation.service.api.TransactionsApi;
import com.backbase.stream.TransactionService;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionTaskExecutor;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.repository.TransactionUnitOfWorkRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@EnableConfigurationProperties({
    BackbaseStreamConfigurationProperties.class,
    TransactionWorkerConfigurationProperties.class
})
@AllArgsConstructor
@Configuration
@Import({DbsWebClientConfiguration.class})
public class TransactionServiceConfiguration {


    @Bean
    public TransactionTaskExecutor transactionTaskExecutor(ApiClient transactionPresentationApiClient) {
        TransactionsApi transactionsApi = new TransactionsApi(transactionPresentationApiClient);
        return new TransactionTaskExecutor(transactionsApi);
    }

    @Bean
    public TransactionUnitOfWorkExecutor transactionUnitOfWorkExecutor(TransactionTaskExecutor transactionTaskExecutor,
        TransactionUnitOfWorkRepository transactionUnitOfWorkRepository,
        TransactionWorkerConfigurationProperties transactionWorkerConfigurationProperties) {

        return new TransactionUnitOfWorkExecutor(transactionUnitOfWorkRepository, transactionTaskExecutor,
            transactionWorkerConfigurationProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public TransactionUnitOfWorkRepository transactionUnitOfWorkRepository() {
        return new InMemoryTransactionUnitOfWorkRepository();
    }

    public static class InMemoryTransactionUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<TransactionTask> implements TransactionUnitOfWorkRepository {

    }

    @Bean
    public TransactionService transactionService(ApiClient transactionPresentationApiClient,
        TransactionUnitOfWorkExecutor transactionTaskExecutor) {
        TransactionsApi transactionsApi = new TransactionsApi(transactionPresentationApiClient);

        return new TransactionService(transactionsApi, transactionTaskExecutor);
    }


    @Bean
    public ApiClient transactionPresentationApiClient(ObjectMapper objectMapper,
        DateFormat dateFormat,
        WebClient dbsWebClient,
        BackbaseStreamConfigurationProperties config) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(config.getDbs().getTransactionPresentationBaseUrl());
        return apiClient;
    }

}
