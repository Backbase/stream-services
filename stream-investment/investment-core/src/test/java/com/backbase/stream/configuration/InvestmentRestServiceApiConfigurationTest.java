package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.backbase.investment.api.service.sync.v1.AssetUniverseApi;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link InvestmentRestServiceApiConfiguration}.
 *
 * Each bean factory method is invoked directly (no Spring context) to verify
 * it returns a non-null, correctly typed bean. Fields normally injected via
 * @Value / @ConfigurationProperties are set through Lombok-generated setters.
 */
@DisplayName("InvestmentRestServiceApiConfiguration")
class InvestmentRestServiceApiConfigurationTest {

    private InvestmentRestServiceApiConfiguration config;
    private com.backbase.investment.api.service.sync.ApiClient syncApiClient;

    @BeforeEach
    void setUp() {
        config = new InvestmentRestServiceApiConfiguration();
        config.setScheme("http");
        config.setServiceId("investment");
        config.setServiceUrl("");
        syncApiClient = mock(com.backbase.investment.api.service.sync.ApiClient.class);
    }

    @Test
    @DisplayName("restInvestmentApiClient returns non-null ApiClient")
    void restInvestmentApiClient_returnsApiClient() {
        com.backbase.investment.api.service.sync.ApiClient result =
            config.restInvestmentApiClient(new RestTemplate(), new ObjectMapper());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("restInvestmentObjectMapper returns a copy of the base ObjectMapper")
    void restInvestmentObjectMapper_returnsCopiedObjectMapper() {
        ObjectMapper base = new ObjectMapper();
        ObjectMapper result = config.restInvestmentObjectMapper(base);
        assertThat(result).isNotNull().isNotSameAs(base);
    }

    @Test
    @DisplayName("restContentApi returns non-null ContentApi")
    void restContentApi_returnsContentApi() {
        assertThat(config.restContentApi(syncApiClient))
            .isNotNull().isInstanceOf(ContentApi.class);
    }

    @Test
    @DisplayName("restAssetUniverseApi returns non-null AssetUniverseApi")
    void restAssetUniverseApi_returnsAssetUniverseApi() {
        assertThat(config.restAssetUniverseApi(syncApiClient))
            .isNotNull().isInstanceOf(AssetUniverseApi.class);
    }

    @Test
    @DisplayName("investmentNewsContentService returns non-null InvestmentRestNewsContentService")
    void investmentNewsContentService_returnsNewsContentService() {
        assertThat(config.investmentNewsContentService(mock(ContentApi.class), syncApiClient))
            .isNotNull().isInstanceOf(InvestmentRestNewsContentService.class);
    }

    @Test
    @DisplayName("investmentRestContentDocumentService returns non-null InvestmentRestDocumentContentService")
    void investmentRestContentDocumentService_returnsDocumentContentService() {
        assertThat(config.investmentRestContentDocumentService(mock(ContentApi.class), syncApiClient))
            .isNotNull().isInstanceOf(InvestmentRestDocumentContentService.class);
    }

    @Test
    @DisplayName("investmentRestAssetUniverseService returns non-null InvestmentRestAssetUniverseService")
    void investmentRestAssetUniverseService_returnsAssetUniverseService() {
        assertThat(config.investmentRestAssetUniverseService(syncApiClient, new IngestConfigProperties()))
            .isNotNull().isInstanceOf(InvestmentRestAssetUniverseService.class);
    }

    @Test
    @DisplayName("investmentRestModelPortfolioService returns non-null InvestmentRestModelPortfolioService")
    void investmentRestModelPortfolioService_returnsModelPortfolioService() {
        assertThat(config.investmentRestModelPortfolioService(syncApiClient))
            .isNotNull().isInstanceOf(InvestmentRestModelPortfolioService.class);
    }
}

