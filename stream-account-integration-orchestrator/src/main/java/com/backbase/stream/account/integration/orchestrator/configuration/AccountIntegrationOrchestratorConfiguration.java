package com.backbase.stream.account.integration.orchestrator.configuration;

import com.backbase.stream.dbs.account.integration.outbound.ApiClient;
import com.backbase.stream.dbs.account.integration.outbound.api.ArrangementDetailsApi;
import com.backbase.stream.dbs.account.integration.outbound.api.ArrangementsApi;
import com.backbase.stream.dbs.account.integration.outbound.api.BalancesApi;
import com.backbase.stream.dbs.account.integration.outbound.api.RecipientArrangementIdsApi;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AccountIntegrationOrchestratorConfigurationProperties.class)
@Import(DbsWebClientConfiguration.class)
@Slf4j
public class AccountIntegrationOrchestratorConfiguration {

    @Bean
    public BalancesApi defaultBalancesApi(ObjectMapper objectMapper,
                                          DateFormat dateFormat,
                                          WebClient dbsWebClient,
                                          AccountIntegrationOrchestratorConfigurationProperties properties) {
        String baseUrl = properties.getDefaultAccountIntegrationBaseUrl();
        log.info("Setting up Default Balance API to: {}", baseUrl);
        return new BalancesApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl));
    }

    @Bean(name = "productTypeBalancesApi")
    public Map<String, BalancesApi> productTypeBalancesApi(ObjectMapper objectMapper,
                                                           DateFormat dateFormat,
                                                           WebClient dbsWebClient,
                                                           AccountIntegrationOrchestratorConfigurationProperties properties) {
        Map<String, BalancesApi> clients = new HashMap<>();
        properties.getProductTypeAccountIntegrationBaseUrl().forEach((productType, baseUrl) -> {
            log.info("Setting up Balance API for Product Type: {} to: {}", productType, baseUrl);
            clients.put(productType, new BalancesApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl)));
        });

        return clients;
    }

    @Bean
    public ArrangementDetailsApi defaultArrangementDetailsApi(ObjectMapper objectMapper,
                                          DateFormat dateFormat,
                                          WebClient dbsWebClient,
                                          AccountIntegrationOrchestratorConfigurationProperties properties) {
        String baseUrl = properties.getDefaultAccountIntegrationBaseUrl();
        log.info("Setting up default Arrangement Details  API to: {}", baseUrl);
        return new ArrangementDetailsApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl));
    }

    @Bean(name = "productTypeArrangementDetailsApi")
    public Map<String, ArrangementDetailsApi> productTypeArrangementDetailsApi(ObjectMapper objectMapper,
                                                                               DateFormat dateFormat,
                                                                               WebClient dbsWebClient,
                                                                               AccountIntegrationOrchestratorConfigurationProperties properties) {
        Map<String, ArrangementDetailsApi> clients = new HashMap<>();
        properties.getProductTypeAccountIntegrationBaseUrl().forEach((productType, baseUrl) -> {
            log.info("Setting up Arrangement Details API for Product Type: {} to: {}", productType, baseUrl);
            clients.put(productType, new ArrangementDetailsApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl)));
        });

        return clients;
    }


    @Bean
    public ArrangementsApi defaultArrangementsApi(ObjectMapper objectMapper,
                                                     DateFormat dateFormat,
                                                     WebClient dbsWebClient,
                                                     AccountIntegrationOrchestratorConfigurationProperties properties) {
        String baseUrl = properties.getDefaultAccountIntegrationBaseUrl();
        log.info("Setting up default Arrangements API to: {}", baseUrl);
        return new ArrangementsApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl));
    }

    @Bean(name = "productTypeArrangementsApi")
    public Map<String, ArrangementsApi> productTypeArrangementsApi(ObjectMapper objectMapper,
                                                                               DateFormat dateFormat,
                                                                               WebClient dbsWebClient,
                                                                               AccountIntegrationOrchestratorConfigurationProperties properties) {
        Map<String, ArrangementsApi> clients = new HashMap<>();
        properties.getProductTypeAccountIntegrationBaseUrl().forEach((productType, baseUrl) -> {
            log.info("Setting up Arrangements API for Product Type: {} to: {}", productType, baseUrl);
            clients.put(productType, new ArrangementsApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl)));
        });

        return clients;
    }


    @Bean
    public RecipientArrangementIdsApi defaultRecipientArrangementIdsApi(ObjectMapper objectMapper,
                                                                           DateFormat dateFormat,
                                                                           WebClient dbsWebClient,
                                                                           AccountIntegrationOrchestratorConfigurationProperties properties) {
        String baseUrl = properties.getDefaultAccountIntegrationBaseUrl();
        log.info("Setting up default Recipient ArrangementIds API to: {}", baseUrl);
        return new RecipientArrangementIdsApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl));
    }

    @Bean(name = "productTypeRecipientArrangementIdsApi")
    public Map<String, RecipientArrangementIdsApi> productTypeRecipientArrangementIdsApi(ObjectMapper objectMapper,
                                                                   DateFormat dateFormat,
                                                                   WebClient dbsWebClient,
                                                                   AccountIntegrationOrchestratorConfigurationProperties properties) {
        Map<String, RecipientArrangementIdsApi> clients = new HashMap<>();
        properties.getProductTypeAccountIntegrationBaseUrl().forEach((productType, baseUrl) -> {
            log.info("Setting up Recipient ArrangementIds API for Product Type: {} to: {}", productType, baseUrl);
            clients.put(productType, new RecipientArrangementIdsApi(getApiClient(objectMapper, dateFormat, dbsWebClient, baseUrl)));
        });

        return clients;
    }

    @NotNull
    private ApiClient getApiClient(ObjectMapper objectMapper, DateFormat dateFormat, WebClient dbsWebClient, String baseUrl) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(baseUrl);
        return apiClient;
    }


    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.authorizeExchange()
            .anyExchange()
            .permitAll()
            .and()
            .csrf()
            .disable()
            .build();
    }

}
