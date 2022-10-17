package com.backbase.stream.portfolio.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * PortfolioSagaConfiguration Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class PortfolioSagaConfigurationTest {

    @Test
    void shouldCreatePortfolioSagas() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
                UserConfigurations.of(PortfolioSagaConfiguration.class, LimitedAutoConfiguration.class));

        contextRunner.run((context) -> {
            Throwable startupFailure = context.getStartupFailure();

            Assertions.assertNull(startupFailure);
            Assertions.assertTrue(context.containsBean("portfolioSaga"));
            Assertions.assertTrue(context.containsBean("wealthRegionsSaga"));
            Assertions.assertTrue(context.containsBean("wealthPortfolioAllocationsSaga"));
            Assertions.assertTrue(context.containsBean("wealthAssetsSaga"));
            Assertions.assertTrue(context.containsBean("wealthPortfolioSaga"));
            Assertions.assertTrue(context.containsBean("wealthSubPortfolioSaga"));
        });
    }

    @SpringBootApplication(scanBasePackages = {"com.backbase.stream.portfolio"})
    public static class LimitedAutoConfiguration {
    }
}
