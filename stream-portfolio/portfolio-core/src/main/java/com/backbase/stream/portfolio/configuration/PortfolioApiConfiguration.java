package com.backbase.stream.portfolio.configuration;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConfigurationProperties("backbase.communication.services.portfolio")
public class PortfolioApiConfiguration extends ApiClientConfig {

    public static final String PORTFOLIO_SERVICE_ID = "portfolio";

    public PortfolioApiConfiguration() {
        super(PORTFOLIO_SERVICE_ID);
    }

    @Bean
    public ApiClient portfolioApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper,
            dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public TransactionCategoryManagementApi transactionCategoryManagementApi(ApiClient portfolioApiClient) {
        return new TransactionCategoryManagementApi(portfolioApiClient);
    }

    @Bean
    public TransactionManagementApi transactionManagementApi(ApiClient portfolioApiClient) {
        return new TransactionManagementApi(portfolioApiClient);
    }

    @Bean
    public PositionManagementApi positionManagementApi(ApiClient portfolioApiClient) {
        return new PositionManagementApi(portfolioApiClient);
    }

    @Bean
    public AggregatePortfolioManagementApi aggregatePortfolioManagementApi(ApiClient portfolioApiClient) {
        return new AggregatePortfolioManagementApi(portfolioApiClient);
    }

    @Bean
    public PortfolioValuationManagementApi portfolioValuationManagementApi(ApiClient portfolioApiClient) {
        return new PortfolioValuationManagementApi(portfolioApiClient);
    }

    @Bean
    public PortfolioPositionsHierarchyManagementApi portfolioPositionsHierarchyManagementApi(
        ApiClient portfolioApiClient) {
        return new PortfolioPositionsHierarchyManagementApi(portfolioApiClient);
    }

    @Bean
    public PortfolioBenchmarksManagementApi portfolioBenchmarksManagementApi(ApiClient portfolioApiClient) {
        return new PortfolioBenchmarksManagementApi(portfolioApiClient);
    }

    @Bean
    public PortfolioCumulativePerformanceManagementApi portfolioCumulativePerformanceManagementApi(
        ApiClient portfolioApiClient) {

        return new PortfolioCumulativePerformanceManagementApi(portfolioApiClient);
    }

    @Bean
    public PortfolioManagementApi portfolioManagementApi(ApiClient portfolioApiClient) {
        return new PortfolioManagementApi(portfolioApiClient);
    }

    @Bean
    public SubPortfolioManagementApi subPortfolioManagementApi(ApiClient portfolioApiClient) {
        return new SubPortfolioManagementApi(portfolioApiClient);
    }

}
