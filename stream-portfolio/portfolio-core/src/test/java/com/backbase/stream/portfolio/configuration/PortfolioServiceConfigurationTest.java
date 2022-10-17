package com.backbase.stream.portfolio.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * PortfolioServiceConfiguration Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class PortfolioServiceConfigurationTest {

	@Test
	void shouldCreatePortfolioSagas() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
				UserConfigurations.of(PortfolioServiceConfiguration.class, LimitedAutoConfiguration.class));

		contextRunner.run((context) -> {
			Throwable startupFailure = context.getStartupFailure();

			Assertions.assertNull(startupFailure);
			Assertions.assertTrue(context.containsBean("portfolioReactiveService"));
			Assertions.assertTrue(context.containsBean("wealthRegionsReactiveService"));
			Assertions.assertTrue(context.containsBean("wealthPortfolioAllocationsReactiveService"));
			Assertions.assertTrue(context.containsBean("wealthAssetsReactiveService"));
			Assertions.assertTrue(context.containsBean("wealthPortfolioReactiveService"));
			Assertions.assertTrue(context.containsBean("wealthSubPortfolioReactiveService"));
		});
	}

	@SpringBootApplication(scanBasePackages = { "com.backbase.stream.portfolio" })
	public static class LimitedAutoConfiguration {
	}
}
