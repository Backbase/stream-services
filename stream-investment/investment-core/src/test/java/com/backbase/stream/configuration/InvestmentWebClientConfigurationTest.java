package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Unit tests for {@link InvestmentWebClientConfiguration}.
 *
 * <p>Each {@code @Bean} factory method is invoked directly (no Spring context) to verify
 * that it returns a non-null, correctly typed bean.
 */
@DisplayName("InvestmentWebClientConfiguration")
class InvestmentWebClientConfigurationTest {

    private InvestmentWebClientConfiguration config;

    @BeforeEach
    void setUp() {
        config = new InvestmentWebClientConfiguration();
    }

    @Test
    @DisplayName("investmentWebClientProperties — returns non-null properties with defaults")
    void investmentWebClientProperties_returnsNonNullProperties() {
        InvestmentWebClientProperties properties = config.investmentWebClientProperties();

        assertThat(properties).isNotNull();
        assertThat(properties.getMaxConnections()).isPositive();
        assertThat(properties.getConnectTimeoutSeconds()).isPositive();
    }

    @Test
    @DisplayName("investmentConnectionProvider — returns configured connection provider")
    void investmentConnectionProvider_returnsConfiguredProvider() {
        InvestmentWebClientProperties properties = config.investmentWebClientProperties();

        ConnectionProvider provider = config.investmentConnectionProvider(properties);

        assertThat(provider).isNotNull();
    }

    @Test
    @DisplayName("investmentHttpClient — returns configured HttpClient")
    void investmentHttpClient_returnsConfiguredHttpClient() {
        InvestmentWebClientProperties properties = config.investmentWebClientProperties();
        ConnectionProvider provider = config.investmentConnectionProvider(properties);

        HttpClient httpClient = config.investmentHttpClient(provider, properties);

        assertThat(httpClient).isNotNull();
    }
}
