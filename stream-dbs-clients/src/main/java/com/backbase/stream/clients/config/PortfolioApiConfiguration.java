package com.backbase.stream.clients.config;

import com.backbase.portfolio.integration.api.service.ApiClient;
import com.backbase.portfolio.integration.api.service.v1.AggregatePortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioBenchmarksManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioCumulativePerformanceManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioPositionsHierarchyManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioValuationManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PositionManagementApi;
import com.backbase.portfolio.integration.api.service.v1.SubPortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.TransactionCategoryManagementApi;
import com.backbase.portfolio.integration.api.service.v1.TransactionManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.portfolio")
public class PortfolioApiConfiguration extends CompositeApiClientConfig {

    public static final String PORTFOLIO_SERVICE_ID = "portfolio";

    public PortfolioApiConfiguration() {
        super(PORTFOLIO_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient portfolioApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionCategoryManagementApi transactionCategoryManagementApi(ApiClient portfolioApiClient) {
        return new TransactionCategoryManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionManagementApi transactionManagementApi(ApiClient portfolioApiClient) {
        return new TransactionManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PositionManagementApi positionManagementApi(ApiClient portfolioApiClient) {
        return new PositionManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public AggregatePortfolioManagementApi aggregatePortfolioManagementApi(ApiClient portfolioApiClient) {
        return new AggregatePortfolioManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioValuationManagementApi portfolioValuationManagementApi(ApiClient portfolioApiClient) {
        return new PortfolioValuationManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioPositionsHierarchyManagementApi portfolioPositionsHierarchyManagementApi(
        ApiClient portfolioApiClient) {
        return new PortfolioPositionsHierarchyManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioBenchmarksManagementApi portfolioBenchmarksManagementApi(ApiClient portfolioApiClient) {
        return new PortfolioBenchmarksManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioCumulativePerformanceManagementApi portfolioCumulativePerformanceManagementApi(
        ApiClient portfolioApiClient) {

        return new PortfolioCumulativePerformanceManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioManagementApi portfolioManagementApi(ApiClient portfolioApiClient) {
        return new PortfolioManagementApi(portfolioApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public SubPortfolioManagementApi subPortfolioManagementApi(ApiClient portfolioApiClient) {
        return new SubPortfolioManagementApi(portfolioApiClient);
    }

}
