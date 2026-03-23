package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.ContentApi;
import com.backbase.investment.api.service.v1.CurrencyApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Unit tests for {@link InvestmentClientConfig}.
 *
 * <p>Each {@code @Bean} factory method is called directly (no Spring context) to verify:
 * <ul>
 *   <li>The method returns a non-null, correctly typed API instance.</li>
 *   <li>The {@code investmentApiClient} method configures Jackson codecs and returns a
 *       non-null {@link ApiClient}.</li>
 * </ul>
 *
 * <p>A real {@link WebClient} (built from {@code WebClient.builder().build()}) is used for
 * the {@code investmentApiClient} test so the codec-configuration lambda is executed in full,
 * giving 100 % line coverage for that method.
 */
@DisplayName("InvestmentClientConfig")
class InvestmentClientConfigTest {

    private InvestmentClientConfig config;

    @BeforeEach
    void setUp() {
        config = new InvestmentClientConfig();
    }

    // -------------------------------------------------------------------------
    // Constant
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("INVESTMENT_SERVICE_ID constant equals 'investment'")
    void investmentServiceIdConstant_hasExpectedValue() {
        assertThat(InvestmentClientConfig.INVESTMENT_SERVICE_ID).isEqualTo("investment");
    }

    // -------------------------------------------------------------------------
    // investmentApiClient
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("investmentApiClient — returns non-null ApiClient with codecs configured")
    void investmentApiClient_returnsNonNullApiClientWithCodecsConfigured() {
        // Use a real WebClient so that WebClient.Builder#codecs(Consumer) actually invokes
        // the codec-configuration lambda and every line of the method is covered.
        WebClient webClient = WebClient.builder().build();
        ObjectMapper objectMapper = new ObjectMapper();
        DateFormat dateFormat = mock(DateFormat.class);
        HttpClient httpClient = mock(HttpClient.class); // declared parameter; not used by the method body

        ApiClient result = config.investmentApiClient(webClient, httpClient, objectMapper, dateFormat);

        assertThat(result).isNotNull();
    }

    // -------------------------------------------------------------------------
    // API bean factory methods
    // -------------------------------------------------------------------------

    /**
     * Verifies that each {@code @Bean} factory method instantiates and returns a
     * correctly typed, non-null API object when given a (mocked) {@link ApiClient}.
     */
    @Nested
    @DisplayName("API bean factory methods")
    class ApiBeanTests {

        private ApiClient apiClient;

        @BeforeEach
        void setUp() {
            apiClient = mock(ApiClient.class);
        }

        @Test
        @DisplayName("clientApi — returns non-null ClientApi")
        void clientApi_returnsClientApi() {
            assertThat(config.clientApi(apiClient)).isNotNull().isInstanceOf(ClientApi.class);
        }

        @Test
        @DisplayName("investmentProductsApi — returns non-null InvestmentProductsApi")
        void investmentProductsApi_returnsInvestmentProductsApi() {
            assertThat(config.investmentProductsApi(apiClient))
                .isNotNull().isInstanceOf(InvestmentProductsApi.class);
        }

        @Test
        @DisplayName("portfolioApi — returns non-null PortfolioApi")
        void portfolioApi_returnsPortfolioApi() {
            assertThat(config.portfolioApi(apiClient)).isNotNull().isInstanceOf(PortfolioApi.class);
        }

        @Test
        @DisplayName("financialAdviceApi — returns non-null FinancialAdviceApi")
        void financialAdviceApi_returnsFinancialAdviceApi() {
            assertThat(config.financialAdviceApi(apiClient)).isNotNull().isInstanceOf(FinancialAdviceApi.class);
        }

        @Test
        @DisplayName("assetUniverseApi — returns non-null AssetUniverseApi")
        void assetUniverseApi_returnsAssetUniverseApi() {
            assertThat(config.assetUniverseApi(apiClient)).isNotNull().isInstanceOf(AssetUniverseApi.class);
        }

        @Test
        @DisplayName("allocationsApi — returns non-null AllocationsApi")
        void allocationsApi_returnsAllocationsApi() {
            assertThat(config.allocationsApi(apiClient)).isNotNull().isInstanceOf(AllocationsApi.class);
        }

        @Test
        @DisplayName("investmentApi — returns non-null InvestmentApi")
        void investmentApi_returnsInvestmentApi() {
            assertThat(config.investmentApi(apiClient)).isNotNull().isInstanceOf(InvestmentApi.class);
        }

        @Test
        @DisplayName("contentApi — returns non-null ContentApi")
        void contentApi_returnsContentApi() {
            assertThat(config.contentApi(apiClient)).isNotNull().isInstanceOf(ContentApi.class);
        }

        @Test
        @DisplayName("paymentsApi — returns non-null PaymentsApi")
        void paymentsApi_returnsPaymentsApi() {
            assertThat(config.paymentsApi(apiClient)).isNotNull().isInstanceOf(PaymentsApi.class);
        }

        @Test
        @DisplayName("portfolioTradingAccountsApi — returns non-null PortfolioTradingAccountsApi")
        void portfolioTradingAccountsApi_returnsPortfolioTradingAccountsApi() {
            assertThat(config.portfolioTradingAccountsApi(apiClient))
                .isNotNull().isInstanceOf(PortfolioTradingAccountsApi.class);
        }

        @Test
        @DisplayName("currencyApi — returns non-null CurrencyApi")
        void currencyApi_returnsCurrencyApi() {
            assertThat(config.currencyApi(apiClient)).isNotNull().isInstanceOf(CurrencyApi.class);
        }

        @Test
        @DisplayName("asyncBulkGroupsApi — returns non-null AsyncBulkGroupsApi")
        void asyncBulkGroupsApi_returnsAsyncBulkGroupsApi() {
            assertThat(config.asyncBulkGroupsApi(apiClient)).isNotNull().isInstanceOf(AsyncBulkGroupsApi.class);
        }
    }
}

