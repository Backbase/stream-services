package com.backbase.stream.portfolio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.backbase.stream.portfolio.saga.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.saga.region.RegionBundleSaga;
import com.backbase.stream.portfolio.service.PortfolioService;
import com.backbase.stream.portfolio.service.RegionService;
import com.backbase.stream.portfolio.service.impl.PortfolioReactiveService;
import com.backbase.stream.portfolio.service.impl.RegionReactiveService;

/**
 * @author Vladimir Kirchev
 *
 */
@Configuration
@EnableConfigurationProperties({ PortfolioSagaProperties.class })
public class PortfolioServiceConfiguration {

	@Bean
	PortfolioService portfolioReactiveService(PortfolioSagaProperties portfolioSagaProperties,
			PortfolioSaga portfolioSaga) {
		return new PortfolioReactiveService(portfolioSagaProperties, portfolioSaga);
	}
	
	@Bean
	RegionService regionReactiveService(PortfolioSagaProperties portfolioSagaProperties,
			RegionBundleSaga regionBundleSaga) {
		return new RegionReactiveService(portfolioSagaProperties, regionBundleSaga);
	}
}
