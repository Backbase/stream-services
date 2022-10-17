package com.backbase.stream.portfolio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.portfolio.saga.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.saga.wealth.allocation.WealthPortfolioAllocationsSaga;
import com.backbase.stream.portfolio.saga.wealth.asset.WealthAssetsSaga;
import com.backbase.stream.portfolio.saga.wealth.portfolio.WealthPortfolioSaga;
import com.backbase.stream.portfolio.saga.wealth.region.WealthRegionsSaga;
import com.backbase.stream.portfolio.saga.wealth.subportfolio.WealthSubPortfolioSaga;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.webclient.DbsWebClientConfiguration;

/**
 * PortfolioSaga Configuration.
 * 
 * @author Vladimir Kirchev
 *
 */
@Configuration
@Import({DbsWebClientConfiguration.class})
@EnableConfigurationProperties({BackbaseStreamConfigurationProperties.class})
public class PortfolioSagaConfiguration {

    @Bean
    PortfolioSaga portfolioSaga(PortfolioIntegrationService portfolioIntegrationService,
            InstrumentIntegrationService instrumentIntegrationService) {
        return new PortfolioSaga(portfolioIntegrationService, instrumentIntegrationService);
    }

    @Bean
    WealthRegionsSaga wealthRegionsSaga(InstrumentIntegrationService instrumentIntegrationService) {
        return new WealthRegionsSaga(instrumentIntegrationService);
    }

    @Bean
    WealthPortfolioAllocationsSaga wealthPortfolioAllocationsSaga(
            PortfolioIntegrationService portfolioIntegrationService) {
        return new WealthPortfolioAllocationsSaga(portfolioIntegrationService);
    }

    @Bean
    WealthAssetsSaga wealthAssetsSaga(InstrumentIntegrationService instrumentIntegrationService) {
        return new WealthAssetsSaga(instrumentIntegrationService);
    }

    @Bean
    WealthPortfolioSaga wealthPortfolioSaga(PortfolioIntegrationService portfolioIntegrationService) {
        return new WealthPortfolioSaga(portfolioIntegrationService);
    }
    
    @Bean
    WealthSubPortfolioSaga wealthSubPortfolioSaga(PortfolioIntegrationService portfolioIntegrationService) {
        return new WealthSubPortfolioSaga(portfolioIntegrationService);
    }
}
