package com.backbase.stream.portfolio.configuration;

import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * PortfolioSaga Configuration.
 *
 * @author Vladimir Kirchev
 */
@Configuration
@Import(DbsApiClientsAutoConfiguration.class)
public class PortfolioSagaConfiguration {

  @Bean
  PortfolioSaga portfolioSaga(
      PortfolioIntegrationService portfolioIntegrationService,
      InstrumentIntegrationService instrumentIntegrationService) {
    return new PortfolioSaga(portfolioIntegrationService, instrumentIntegrationService);
  }
}
