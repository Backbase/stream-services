package com.backbase.stream.portfolio.configuration;

import static org.mockito.Mockito.when;

import java.text.DateFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.backbase.portfolio.instrument.integration.api.service.ApiClient;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties.DbsConnectionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
abstract class BaseApiConfigurationTest<Config, Client> {

    public static final String PORTFOLIO_BASE_URL = "portfolioBaseUrl";
    @Mock
    WebClient dbsWebClient;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    DateFormat dateFormat;
    @Mock
    BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;
    @Mock
    DbsConnectionProperties dbsConnectionProperties;

    Config config;
    Client client;

    InstrumentApiConfiguration instrumentApiConfiguration;
    PortfolioApiConfiguration portfolioApiConfiguration;
    ApiClient apiClient;

    @BeforeEach
    void init() {
        when(backbaseStreamConfigurationProperties.getDbs()).thenReturn(dbsConnectionProperties);
        when(dbsConnectionProperties.getPortfolioBaseUrl()).thenReturn(PORTFOLIO_BASE_URL);

        instrumentApiConfiguration = new InstrumentApiConfiguration(backbaseStreamConfigurationProperties);
        apiClient = instrumentApiConfiguration.instrumentApiClient(dbsWebClient, objectMapper, dateFormat);
    }

    protected void assertBaseUrl(Client client) {
        Assertions.assertEquals(PORTFOLIO_BASE_URL, getBasePath(client));
    }

    public abstract Client getClient();

    public abstract Config getConfig();

    public abstract String getBasePath(Client client);

}