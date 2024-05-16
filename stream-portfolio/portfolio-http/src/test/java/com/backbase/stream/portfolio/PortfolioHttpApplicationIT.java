package com.backbase.stream.portfolio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PortfolioHttpApplication Test.
 *
 * @author Vladimir Kirchev
 *
 */
class PortfolioHttpApplicationIT {

	@Test
	void shouldStartApplication() {
		Assertions.assertDoesNotThrow(() -> PortfolioHttpApplication.main(new String[] {"--spring.cloud.config.enabled=false"}));
	}
}
