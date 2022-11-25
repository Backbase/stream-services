package com.backbase.stream.portfolio.configuration;

import java.time.Duration;

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
	
	@Test
	void shouldNotBeEqual() {
		PortfolioSagaProperties portfolioSagaProperties1 = new PortfolioSagaProperties();
		PortfolioSagaProperties portfolioSagaProperties2 = new PortfolioSagaProperties();
		portfolioSagaProperties2.setBufferSize(1000);
		portfolioSagaProperties2.setMaxRetries(10);
		portfolioSagaProperties2.setRateLimit(11111);
		portfolioSagaProperties2.setTaskExecutors(44);
		portfolioSagaProperties2.setBufferMaxTime(Duration.ofSeconds(55));
		portfolioSagaProperties2.setRetryDuration(Duration.ofSeconds(66));
		
		Assertions.assertNotEquals(portfolioSagaProperties1, portfolioSagaProperties2);
	}
}
