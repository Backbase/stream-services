package com.backbase.stream.transactions;

import com.backbase.dbs.transaction.presentation.service.ApiClient;
import com.backbase.dbs.transaction.presentation.service.api.TransactionsApi;
import com.backbase.stream.AbstractServiceIntegrationTests;
import com.backbase.stream.TransactionService;
import com.backbase.stream.configuration.TransactionWorkerConfigurationProperties;
import com.backbase.stream.transaction.TransactionTaskExecutor;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.generator.TransactionGenerator;
import com.backbase.stream.transaction.generator.configuration.TransactionGeneratorOptions;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import org.junit.Before;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class AbstractTransactionServiceTests extends AbstractServiceIntegrationTests {

    protected TransactionService transactionService;
    protected TransactionTaskExecutor transactionTaskExecutor;
    protected TransactionUnitOfWorkExecutor transactionUnitOfWorkExecutor;
    protected TransactionGenerator transactionsDataGenerator;

    @Before
    public void setup() {
        String tokenUri = "https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token";
        WebClient webClient = super.setupWebClientBuilder(tokenUri, "bb-client", "bb-secret");
        TransactionsApi transactionsApi = new TransactionsApi(transactionPresentationApiClient(webClient));

        transactionTaskExecutor = new TransactionTaskExecutor(transactionsApi);
        TransactionWorkerConfigurationProperties properties = new TransactionWorkerConfigurationProperties();
        transactionUnitOfWorkExecutor = new TransactionUnitOfWorkExecutor(new InMemoryReactiveUnitOfWorkRepository<>(), transactionTaskExecutor, properties);
        transactionsDataGenerator = new TransactionGenerator(new TransactionGeneratorOptions());
        transactionService  = new TransactionService(transactionsApi, transactionUnitOfWorkExecutor);
    }


    public ApiClient transactionPresentationApiClient(WebClient webClient) {
        ApiClient apiClient = new ApiClient(webClient, getObjectMapper(), getDateFormat());
        String basePath = "https://stream-api.proto.backbasecloud.com/transaction-presentation-service/service-api/v2";
        apiClient.setBasePath(basePath);
        return apiClient;
    }

}
