package com.backbase.stream.portfolio.configuration;

import com.backbase.portfolio.instrument.integration.api.service.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
abstract class BaseApiConfigurationTest<Config, Client> {

    public static final String PORTFOLIO_BASE_URL = "null://portfolio";
    @Mock
    WebClient dbsWebClient;

    ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    DateFormat dateFormat;

    Config config;
    Client client;

    InstrumentApiConfiguration instrumentApiConfiguration;
    PortfolioApiConfiguration portfolioApiConfiguration;
    ApiClient apiClient;

    @BeforeEach
    void init() {
        instrumentApiConfiguration = new InstrumentApiConfiguration();
        instrumentApiConfiguration.setWebClient(dbsWebClient);
        apiClient = instrumentApiConfiguration.instrumentApiClient(objectMapper, dateFormat);
    }

    protected void assertBaseUrl(Client client) {
        Assertions.assertEquals(PORTFOLIO_BASE_URL, getBasePath(client));
    }

    public abstract Client getClient();

    public abstract Config getConfig();

    public abstract String getBasePath(Client client);

}
