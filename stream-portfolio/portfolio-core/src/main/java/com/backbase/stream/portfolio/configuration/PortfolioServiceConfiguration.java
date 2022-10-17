package com.backbase.stream.portfolio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.backbase.stream.portfolio.saga.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.saga.wealth.allocation.WealthPortfolioAllocationsSaga;
import com.backbase.stream.portfolio.saga.wealth.asset.WealthAssetsSaga;
import com.backbase.stream.portfolio.saga.wealth.portfolio.WealthPortfolioSaga;
import com.backbase.stream.portfolio.saga.wealth.region.WealthRegionsSaga;
import com.backbase.stream.portfolio.saga.wealth.subportfolio.WealthSubPortfolioSaga;
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
            WealthRegionsSaga wealthRegionsSaga) {
        return new WealthRegionsReactiveService(portfolioSagaProperties, wealthRegionsSaga);
    }

    @Bean
    WealthPortfolioAllocationsService wealthPortfolioAllocationsReactiveService(
            PortfolioSagaProperties portfolioSagaProperties,
            WealthPortfolioAllocationsSaga wealthPortfolioAllocationsSaga) {
        return new WealthPortfolioAllocationsReactiveService(portfolioSagaProperties, wealthPortfolioAllocationsSaga);
    }

    @Bean
    WealthAssetsService wealthAssetsReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            WealthAssetsSaga wealthAssetsSaga) {
        return new WealthAssetsReactiveService(portfolioSagaProperties, wealthAssetsSaga);
    }

    @Bean
    WealthPortfolioService wealthPortfolioReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            WealthPortfolioSaga wealthPortfolioSaga) {
        return new WealthPortfolioReactiveService(portfolioSagaProperties, wealthPortfolioSaga);
    }
    
    @Bean
    WealthSubPortfolioService wealthSubPortfolioReactiveService(PortfolioSagaProperties portfolioSagaProperties,
            WealthSubPortfolioSaga wealthSubPortfolioSaga) {
        return new WealthSubPortfolioReactiveService(portfolioSagaProperties, wealthSubPortfolioSaga);
    }
}
