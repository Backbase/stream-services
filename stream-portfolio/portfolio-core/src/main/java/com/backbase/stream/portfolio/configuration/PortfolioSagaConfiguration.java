package com.backbase.stream.portfolio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.portfolio.PortfolioSaga;
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
}
