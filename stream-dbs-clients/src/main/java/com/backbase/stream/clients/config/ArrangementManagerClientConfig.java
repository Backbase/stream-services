package com.backbase.stream.clients.config;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v2.ProductKindsApi;
import com.backbase.dbs.arrangement.api.service.v2.ProductsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.arrangement.manager")
public class ArrangementManagerClientConfig extends ApiClientConfig {

    public static final String ARRANGEMENT_MANAGER_SERVICE_ID = "arrangement-manager";

    public ArrangementManagerClientConfig() {
        super(ARRANGEMENT_MANAGER_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient arrangementManagerApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public ArrangementsApi arrangementsApi(ApiClient arrangementManagerApiClient) {
        return new ArrangementsApi(arrangementManagerApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProductsApi productsApi(ApiClient arrangementManagerApiClient) {
        return new ProductsApi(arrangementManagerApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProductKindsApi productKindsApi(ApiClient arrangementManagerApiClient) {
        return new ProductKindsApi(arrangementManagerApiClient);
    }

}
