package com.backbase.stream.portfolio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.backbase.stream.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioService;
import com.backbase.stream.portfolio.service.WealthAssetsService;
import com.backbase.stream.portfolio.service.WealthPortfolioAllocationsService;
import com.backbase.stream.portfolio.service.WealthPortfolioService;
import com.backbase.stream.portfolio.service.WealthRegionsService;
import com.backbase.stream.portfolio.service.WealthSubPortfolioService;
import com.backbase.stream.portfolio.service.impl.PortfolioReactiveService;
import com.backbase.stream.portfolio.service.impl.WealthAssetsReactiveService;
import com.backbase.stream.portfolio.service.impl.WealthPortfolioAllocationsReactiveService;
import com.backbase.stream.portfolio.service.impl.WealthPortfolioReactiveService;
import com.backbase.stream.portfolio.service.impl.WealthRegionsReactiveService;
import com.backbase.stream.portfolio.service.impl.WealthSubPortfolioReactiveService;

/**
 * @author Vladimir Kirchev
 *
 */
@Configuration
@EnableConfigurationProperties({PortfolioSagaProperties.class})
public class PortfolioServiceConfiguration {

    @Bean
    PortfolioService portfolioReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            PortfolioSaga portfolioSaga) {
        return new PortfolioReactiveService(portfolioSagaProperties, portfolioSaga);
    }

    @Bean
    WealthRegionsService wealthRegionsReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            InstrumentIntegrationService instrumentIntegrationService) {
        return new WealthRegionsReactiveService(portfolioSagaProperties, instrumentIntegrationService);
    }

    @Bean
    WealthPortfolioAllocationsService wealthPortfolioAllocationsReactiveService(
            PortfolioSagaProperties portfolioSagaProperties, PortfolioIntegrationService portfolioIntegrationService) {
        return new WealthPortfolioAllocationsReactiveService(portfolioSagaProperties, portfolioIntegrationService);
    }
    
    @Bean
    WealthAssetsService wealthAssetsReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            InstrumentIntegrationService instrumentIntegrationService) {
        return new WealthAssetsReactiveService(portfolioSagaProperties, instrumentIntegrationService);
    }

    @Bean
    WealthPortfolioService wealthPortfolioReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            PortfolioIntegrationService portfolioIntegrationService) {
        return new WealthPortfolioReactiveService(portfolioSagaProperties, portfolioIntegrationService);
    }

    @Bean
    WealthSubPortfolioService wealthSubPortfolioReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            PortfolioIntegrationService portfolioIntegrationService) {
        return new WealthSubPortfolioReactiveService(portfolioSagaProperties, portfolioIntegrationService);
    }
}
