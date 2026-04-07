package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.CurrencyApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.stream.investment.saga.InvestmentAssetUniverseSaga;
import com.backbase.stream.investment.saga.InvestmentContentSaga;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.AsyncTaskService;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentCurrencyService;
import com.backbase.stream.investment.service.InvestmentIntradayAssetPriceService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.investment.service.InvestmentRiskAssessmentService;
import com.backbase.stream.investment.service.InvestmentRiskQuestionaryService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InvestmentServiceConfiguration}.
 *
 * <p>Each {@code @Bean} factory method is called directly (no Spring context) to verify:
 * <ul>
 *   <li>The method returns a non-null, correctly typed bean instance.</li>
 * </ul>
 *
 * <p>All collaborators are Mockito mocks so no Spring wiring or network calls occur.
 */
@DisplayName("InvestmentServiceConfiguration")
class InvestmentServiceConfigurationTest {

    private InvestmentServiceConfiguration config;

    @BeforeEach
    void setUp() {
        config = new InvestmentServiceConfiguration();
    }

    // -------------------------------------------------------------------------
    // Simple single-arg service beans
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Simple service bean factory methods")
    class SimpleServiceBeans {

        @Test
        @DisplayName("investmentClientService — returns non-null InvestmentClientService")
        void investmentClientService_returnsNonNull() {
            assertThat(config.investmentClientService(mock(ClientApi.class)))
                .isNotNull().isInstanceOf(InvestmentClientService.class);
        }

        @Test
        @DisplayName("asyncTaskService — returns non-null AsyncTaskService")
        void asyncTaskService_returnsNonNull() {
            assertThat(config.asyncTaskService(mock(AsyncBulkGroupsApi.class)))
                .isNotNull().isInstanceOf(AsyncTaskService.class);
        }

        @Test
        @DisplayName("investmentModelPortfolioService — returns non-null InvestmentModelPortfolioService")
        void investmentModelPortfolioService_returnsNonNull() {
            assertThat(config.investmentModelPortfolioService(mock(FinancialAdviceApi.class)))
                .isNotNull().isInstanceOf(InvestmentModelPortfolioService.class);
        }

        @Test
        @DisplayName("investmentAssetPriceService — returns non-null InvestmentAssetPriceService")
        void investmentAssetPriceService_returnsNonNull() {
            assertThat(config.investmentAssetPriceService(mock(AssetUniverseApi.class)))
                .isNotNull().isInstanceOf(InvestmentAssetPriceService.class);
        }

        @Test
        @DisplayName("investmentIntradayAssetPriceService — returns non-null InvestmentIntradayAssetPriceService")
        void investmentIntradayAssetPriceService_returnsNonNull() {
            assertThat(config.investmentIntradayAssetPriceService(mock(AssetUniverseApi.class)))
                .isNotNull().isInstanceOf(InvestmentIntradayAssetPriceService.class);
        }

        @Test
        @DisplayName("investmentCurrencyService — returns non-null InvestmentCurrencyService")
        void investmentCurrencyService_returnsNonNull() {
            assertThat(config.investmentCurrencyService(mock(CurrencyApi.class)))
                .isNotNull().isInstanceOf(InvestmentCurrencyService.class);
        }

        @Test
        @DisplayName("investmentRiskAssessmentService — returns non-null InvestmentRiskAssessmentService")
        void investmentRiskAssessmentService_returnsNonNull() {
            assertThat(config.investmentRiskAssessmentService(mock(RiskAssessmentApi.class)))
                .isNotNull().isInstanceOf(InvestmentRiskAssessmentService.class);
        }
    }

    // -------------------------------------------------------------------------
    // Multi-arg service beans
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Multi-arg service bean factory methods")
    class MultiArgServiceBeans {

        @Test
        @DisplayName("investmentPortfolioService — returns non-null InvestmentPortfolioService")
        void investmentPortfolioService_returnsNonNull() {
            assertThat(config.investmentPortfolioService(
                    mock(PortfolioApi.class),
                    mock(InvestmentProductsApi.class),
                    mock(PaymentsApi.class),
                    mock(PortfolioTradingAccountsApi.class),
                    mock(IngestConfigProperties.class)))
                .isNotNull().isInstanceOf(InvestmentPortfolioService.class);
        }

        @Test
        @DisplayName("investmentAssetUniverseService — returns non-null InvestmentAssetUniverseService")
        void investmentAssetUniverseService_returnsNonNull() {
            assertThat(config.investmentAssetUniverseService(
                    mock(AssetUniverseApi.class),
                    mock(InvestmentRestAssetUniverseService.class)))
                .isNotNull().isInstanceOf(InvestmentAssetUniverseService.class);
        }

        @Test
        @DisplayName("investmentPortfolioAllocationService — returns non-null InvestmentPortfolioAllocationService")
        void investmentPortfolioAllocationService_returnsNonNull() {
            assertThat(config.investmentPortfolioAllocationService(
                    mock(AllocationsApi.class),
                    mock(AssetUniverseApi.class),
                    mock(InvestmentApi.class),
                    mock(IngestConfigProperties.class)))
                .isNotNull().isInstanceOf(InvestmentPortfolioAllocationService.class);
        }

        @Test
        @DisplayName("investmentRiskQuestionaryService — returns non-null InvestmentRiskQuestionaryService")
        void investmentRiskQuestionaryService_returnsNonNull() {
            assertThat(config.investmentRiskQuestionaryService(
                    mock(RiskAssessmentApi.class),
                    mock(IngestConfigProperties.class)))
                .isNotNull().isInstanceOf(InvestmentRiskQuestionaryService.class);
        }
    }

    // -------------------------------------------------------------------------
    // Saga bean factory methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Saga bean factory methods")
    class SagaBeans {

        @Test
        @DisplayName("investmentSaga — returns non-null InvestmentSaga")
        void investmentSaga_returnsNonNull() {
            assertThat(config.investmentSaga(
                    mock(InvestmentClientService.class),
                    mock(InvestmentRiskAssessmentService.class),
                    mock(InvestmentRiskQuestionaryService.class),
                    mock(InvestmentPortfolioService.class),
                    mock(InvestmentModelPortfolioService.class),
                    mock(InvestmentPortfolioAllocationService.class),
                    mock(AsyncTaskService.class),
                    mock(InvestmentIngestionConfigurationProperties.class)))
                .isNotNull().isInstanceOf(InvestmentSaga.class);
        }

        @Test
        @DisplayName("investmentStaticDataSaga — returns non-null InvestmentAssetUniverseSaga")
        void investmentStaticDataSaga_returnsNonNull() {
            assertThat(config.investmentStaticDataSaga(
                    mock(InvestmentAssetUniverseService.class),
                    mock(InvestmentAssetPriceService.class),
                    mock(InvestmentIntradayAssetPriceService.class),
                    mock(InvestmentCurrencyService.class),
                    mock(AsyncTaskService.class),
                    mock(InvestmentIngestionConfigurationProperties.class),
                    mock(IngestConfigProperties.class)))
                .isNotNull().isInstanceOf(InvestmentAssetUniverseSaga.class);
        }

        @Test
        @DisplayName("investmentContentSaga — returns non-null InvestmentContentSaga")
        void investmentContentSaga_returnsNonNull() {
            assertThat(config.investmentContentSaga(
                    mock(InvestmentRestNewsContentService.class),
                    mock(InvestmentRestDocumentContentService.class),
                    mock(InvestmentIngestionConfigurationProperties.class)))
                .isNotNull().isInstanceOf(InvestmentContentSaga.class);
        }
    }
}

