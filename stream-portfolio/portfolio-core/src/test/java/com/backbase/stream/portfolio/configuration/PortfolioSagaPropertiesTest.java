package com.backbase.stream.portfolio.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PortfolioSagaProperties Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class PortfolioSagaPropertiesTest {
	
	@Test
	void shouldGetToString() {
		PortfolioSagaProperties portfolioSagaProperties = new PortfolioSagaProperties();
		
		String toStringValue = portfolioSagaProperties.toString();
		
		Assertions.assertNotNull(toStringValue);
	}
	
	@Test
	void shouldBeEqual() {
		PortfolioSagaProperties portfolioSagaProperties1 = new PortfolioSagaProperties();
		PortfolioSagaProperties portfolioSagaProperties2 = new PortfolioSagaProperties();
		
		Assertions.assertEquals(portfolioSagaProperties1, portfolioSagaProperties2);
	}
}
