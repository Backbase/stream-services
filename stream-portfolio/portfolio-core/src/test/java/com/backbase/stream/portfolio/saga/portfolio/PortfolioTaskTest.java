package com.backbase.stream.portfolio.saga.portfolio;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.util.RegionTestUtil;

class PortfolioTaskTest {

	@Test
	void getName() {
		PortfolioTask portfolioTask = new PortfolioTask(new WealthBundle());
		assertNotNull(portfolioTask.getName());
	}

	@Test
	void shouldBeEqual() {
		PortfolioTask portfolioTask1 = new PortfolioTask(new WealthBundle());
		PortfolioTask portfolioTask2 = new PortfolioTask(new WealthBundle());

		Assertions.assertNotEquals(portfolioTask1, portfolioTask2);
	}

	@Test
	void shouldNotBeEqual() {
		PortfolioTask portfolioTask1 = new PortfolioTask(
				new WealthBundle().addRegionsItem(RegionTestUtil.createRegionBundleUs()));
		PortfolioTask portfolioTask2 = new PortfolioTask(
				new WealthBundle().addRegionsItem(RegionTestUtil.createRegionBundleEu()));

		Assertions.assertNotEquals(portfolioTask1, portfolioTask2);
	}

}