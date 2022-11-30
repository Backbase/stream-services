package com.backbase.stream.portfolio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PortfolioHttpApplication Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class PortfolioHttpApplicationTest {
	
	@Test
	void shouldStartApplication() {
		Assertions.assertDoesNotThrow(() -> PortfolioHttpApplication.main(new String[] {}));
	}
}
