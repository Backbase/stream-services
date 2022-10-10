package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.transaction.api.service.ApiClient;
import com.backbase.dbs.transaction.api.service.v2.TransactionPresentationServiceApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.transaction.manager")
public class TransactionManagerClientConfig extends ApiClientConfig {

    public static final String TRANSACTION_MANAGER_SERVICE_ID = "transaction-manager";

    public TransactionManagerClientConfig() {
        super(TRANSACTION_MANAGER_SERVICE_ID);
    }

    @Bean
    public ApiClient transactionManagerClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public TransactionPresentationServiceApi transactionPresentationServiceApi(ApiClient transactionManagerClient) {
        return new TransactionPresentationServiceApi(transactionManagerClient);
    }

}
