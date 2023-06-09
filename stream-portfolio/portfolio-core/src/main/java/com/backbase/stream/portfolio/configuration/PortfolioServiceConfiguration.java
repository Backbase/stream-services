package com.backbase.stream.portfolio.configuration;

import com.backbase.stream.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.service.InstrumentIngestionService;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIngestionService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioService;
import com.backbase.stream.portfolio.service.impl.InstrumentIngestionReactiveService;
import com.backbase.stream.portfolio.service.impl.PortfolioIngestionReactiveService;
import com.backbase.stream.portfolio.service.impl.PortfolioReactiveService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Kirchev
 */
@Configuration
@EnableConfigurationProperties({PortfolioSagaProperties.class})
public class PortfolioServiceConfiguration {

    @Bean
    PortfolioService portfolioReactiveService(
        PortfolioSagaProperties portfolioSagaProperties, PortfolioSaga portfolioSaga) {
        return new PortfolioReactiveService(portfolioSagaProperties, portfolioSaga);
    }

    @Bean
    PortfolioIngestionService portfolioIngestionReactiveService(
        PortfolioSagaProperties portfolioSagaProperties,
        PortfolioIntegrationService portfolioIntegrationService) {
        return new PortfolioIngestionReactiveService(
            portfolioSagaProperties, portfolioIntegrationService);
    }

    @Bean
    InstrumentIngestionService instrumentIngestionReactiveService(
        PortfolioSagaProperties portfolioSagaProperties,
        InstrumentIntegrationService instrumentIntegrationService) {
        return new InstrumentIngestionReactiveService(
            portfolioSagaProperties, instrumentIntegrationService);
    }
}
