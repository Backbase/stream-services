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
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InvestmentServiceConfiguration}.
 *
 * <p>Each {@code @Bean} factory method is invoked directly (no Spring context) to verify
 * that it returns a non-null, correctly typed bean when provided with mocked or default
 * dependency instances.
 */
@DisplayName("InvestmentServiceConfiguration")
class InvestmentServiceConfigurationTest {

    private InvestmentServiceConfiguration config;

    @BeforeEach
    void setUp() {
        config = new InvestmentServiceConfiguration();
    }

    // -------------------------------------------------------------------------
    // Service bean factory methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("service bean factory methods")
    class ServiceBeans {

        @Test
        @DisplayName("investmentClientService — returns non-null InvestmentClientService")
        void investmentClientService_returnsClientService() {
            assertThat(config.investmentClientService(mock(ClientApi.class)))
                .isNotNull().isInstanceOf(InvestmentClientService.class);
        }

        @Test
        @DisplayName("investmentModelPortfolioService — returns non-null InvestmentModelPortfolioService")
        void investmentModelPortfolioService_returnsModelPortfolioService() {
            assertThat(config.investmentModelPortfolioService(
                    mock(FinancialAdviceApi.class),
                    mock(InvestmentRestModelPortfolioService.class)))
                .isNotNull().isInstanceOf(InvestmentModelPortfolioService.class);
        }

        @Test
        @DisplayName("investmentPortfolioService — returns non-null InvestmentPortfolioService")
        void investmentPortfolioService_returnsPortfolioService() {
            assertThat(config.investmentPortfolioService(
                    mock(PortfolioApi.class),
                    mock(InvestmentProductsApi.class),
                    mock(PaymentsApi.class),
                    mock(PortfolioTradingAccountsApi.class),
                    new IngestConfigProperties()))
                .isNotNull().isInstanceOf(InvestmentPortfolioService.class);
        }

        @Test
        @DisplayName("investmentAssetUniverseService — returns non-null InvestmentAssetUniverseService")
        void investmentAssetUniverseService_returnsAssetUniverseService() {
            assertThat(config.investmentAssetUniverseService(
                    mock(AssetUniverseApi.class),
                    mock(InvestmentRestAssetUniverseService.class)))
                .isNotNull().isInstanceOf(InvestmentAssetUniverseService.class);
        }

        @Test
        @DisplayName("asyncTaskService — returns non-null AsyncTaskService")
        void asyncTaskService_returnsAsyncTaskService() {
            assertThat(config.asyncTaskService(mock(AsyncBulkGroupsApi.class)))
                .isNotNull().isInstanceOf(AsyncTaskService.class);
        }

        @Test
        @DisplayName("investmentAssetPriceService — returns non-null InvestmentAssetPriceService")
        void investmentAssetPriceService_returnsAssetPriceService() {
            assertThat(config.investmentAssetPriceService(mock(AssetUniverseApi.class)))
                .isNotNull().isInstanceOf(InvestmentAssetPriceService.class);
        }

        @Test
        @DisplayName("investmentIntradayAssetPriceService — returns non-null InvestmentIntradayAssetPriceService")
        void investmentIntradayAssetPriceService_returnsIntradayAssetPriceService() {
            assertThat(config.investmentIntradayAssetPriceService(mock(AssetUniverseApi.class)))
                .isNotNull().isInstanceOf(InvestmentIntradayAssetPriceService.class);
        }

        @Test
        @DisplayName("investmentPortfolioAllocationService — returns non-null InvestmentPortfolioAllocationService")
        void investmentPortfolioAllocationService_returnsAllocationService() {
            assertThat(config.investmentPortfolioAllocationService(
                    mock(AllocationsApi.class),
                    mock(AssetUniverseApi.class),
                    mock(InvestmentApi.class),
                    new IngestConfigProperties()))
                .isNotNull().isInstanceOf(InvestmentPortfolioAllocationService.class);
        }

        @Test
        @DisplayName("investmentCurrencyService — returns non-null InvestmentCurrencyService")
        void investmentCurrencyService_returnsCurrencyService() {
            assertThat(config.investmentCurrencyService(mock(CurrencyApi.class)))
                .isNotNull().isInstanceOf(InvestmentCurrencyService.class);
        }

        @Test
        @DisplayName("investmentRiskAssessmentService — returns non-null InvestmentRiskAssessmentService")
        void investmentRiskAssessmentService_returnsRiskAssessmentService() {
            assertThat(config.investmentRiskAssessmentService(mock(RiskAssessmentApi.class)))
                .isNotNull().isInstanceOf(InvestmentRiskAssessmentService.class);
        }

        @Test
        @DisplayName("investmentRiskQuestionaryService — returns non-null InvestmentRiskQuestionaryService")
        void investmentRiskQuestionaryService_returnsRiskQuestionaryService() {
            assertThat(config.investmentRiskQuestionaryService(
                    mock(RiskAssessmentApi.class),
                    new IngestConfigProperties()))
                .isNotNull().isInstanceOf(InvestmentRiskQuestionaryService.class);
        }
    }

    // -------------------------------------------------------------------------
    // Saga bean factory methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("saga bean factory methods")
    class SagaBeans {

        @Test
        @DisplayName("investmentSaga — returns non-null InvestmentSaga")
        void investmentSaga_returnsInvestmentSaga() {
            assertThat(config.investmentSaga(
                    mock(InvestmentClientService.class),
                    mock(InvestmentRiskAssessmentService.class),
                    mock(InvestmentRiskQuestionaryService.class),
                    mock(InvestmentPortfolioService.class),
                    mock(InvestmentModelPortfolioService.class),
                    mock(InvestmentPortfolioAllocationService.class),
                    mock(AsyncTaskService.class),
                    new InvestmentIngestionConfigurationProperties()))
                .isNotNull().isInstanceOf(InvestmentSaga.class);
        }

        @Test
        @DisplayName("investmentStaticDataSaga — returns non-null InvestmentAssetUniverseSaga")
        void investmentStaticDataSaga_returnsAssetUniverseSaga() {
            assertThat(config.investmentStaticDataSaga(
                    mock(InvestmentAssetUniverseService.class),
                    mock(InvestmentAssetPriceService.class),
                    mock(InvestmentIntradayAssetPriceService.class),
                    mock(InvestmentCurrencyService.class),
                    mock(AsyncTaskService.class),
                    new InvestmentIngestionConfigurationProperties(),
                    new IngestConfigProperties()))
                .isNotNull().isInstanceOf(InvestmentAssetUniverseSaga.class);
        }

        @Test
        @DisplayName("investmentContentSaga — returns non-null InvestmentContentSaga")
        void investmentContentSaga_returnsContentSaga() {
            assertThat(config.investmentContentSaga(
                    mock(InvestmentRestNewsContentService.class),
                    mock(InvestmentRestDocumentContentService.class),
                    new InvestmentIngestionConfigurationProperties()))
                .isNotNull().isInstanceOf(InvestmentContentSaga.class);
        }
    }
}


