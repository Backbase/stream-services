package com.backbase.stream.configuration;

import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.dbs.transaction.api.service.ApiClient;
import com.backbase.dbs.transaction.api.service.v2.TransactionPresentationServiceApi;
import com.backbase.stream.TransactionService;
import com.backbase.stream.TransactionServiceImpl;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionTaskExecutor;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.repository.TransactionUnitOfWorkRepository;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class TransactionServiceConfiguration {


    @Bean
    public TransactionTaskExecutor transactionTaskExecutor(ApiClient transactionPresentationApiClient) {
        TransactionPresentationServiceApi transactionsApi = new TransactionPresentationServiceApi(transactionPresentationApiClient);
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
        TransactionPresentationServiceApi transactionsApi = new TransactionPresentationServiceApi(transactionPresentationApiClient);

        return new TransactionServiceImpl(transactionsApi, transactionTaskExecutor);
    }


    @Bean
    public ApiClient transactionPresentationApiClient(ObjectMapper objectMapper,
        DateFormat dateFormat,
        @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        BackbaseStreamConfigurationProperties config) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(config.getDbs().getTransactionManagerBaseUrl());
        return apiClient;
    }

}
