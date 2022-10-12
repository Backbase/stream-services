package com.backbase.stream.portfolio.saga.portfolio;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.util.RegionTestUtil;
import com.backbase.stream.worker.model.StreamTask.State;

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
	
	@Test
	void shouldNotBeEqual_DifferentType() {
		PortfolioTask portfolioTask1 = new PortfolioTask(
				new WealthBundle().addRegionsItem(RegionTestUtil.createRegionBundleUs()));
		
		Assertions.assertNotEquals(portfolioTask1, new Object());
	}
	
	@Test
	void shouldGetHashCode() {
		PortfolioTask portfolioTask1 = new PortfolioTask(
				new WealthBundle().addRegionsItem(RegionTestUtil.createRegionBundleUs()));
		
		Assertions.assertNotEquals(0, portfolioTask1.hashCode());
	}
	
	@Test
	void shouldHaveNoArgsConstructor() {
		PortfolioTask portfolioTask1 = new PortfolioTask();
		
		Assertions.assertNotNull(portfolioTask1);
	}
	
	@Test
	void shouldGetData() {
		PortfolioTask portfolioTask1 = new PortfolioTask(
				new WealthBundle().addRegionsItem(RegionTestUtil.createRegionBundleUs()));
		
		Assertions.assertNotNull(portfolioTask1.getData());
	}

}