package com.backbase.stream.portfolio.saga.region;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.util.RegionTestUtil;

/**
 * RegionBundleTask Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class RegionBundleTaskTest {

	@Test
	void shouldBeProperlyInitialized() {
		RegionBundleTask regionBundleTask = RegionTestUtil.createRegionBundleTaskEu();

		Assertions.assertNotNull(regionBundleTask.getName());

		RegionBundle data = regionBundleTask.getData();

		Assertions.assertNotNull(data);
	}

	@Test
	void shouldGetToString() {
		RegionBundleTask regionBundleTask = RegionTestUtil.createRegionBundleTaskEu();

		String toStringValue = regionBundleTask.toString();

		Assertions.assertNotNull(toStringValue);
	}

	@Test
	void shouldNotBeEqual() {
		RegionBundleTask regionBundleTaskEu = RegionTestUtil.createRegionBundleTaskEu();
		RegionBundleTask regionBundleTaskUs = RegionTestUtil.createRegionBundleTaskUs();

		Assertions.assertNotEquals(regionBundleTaskEu, regionBundleTaskUs);
	}

	@Test
	void shouldNotBeEqual_SameData() {
		RegionBundleTask regionBundleTaskEu1 = RegionTestUtil.createRegionBundleTaskEu();
		RegionBundleTask regionBundleTaskEu2 = RegionTestUtil.createRegionBundleTaskEu();

		Assertions.assertNotEquals(regionBundleTaskEu1, regionBundleTaskEu2);
	}
	
	@Test
	void shouldGetHashCode() {
		RegionBundleTask regionBundleTaskEu1 = RegionTestUtil.createRegionBundleTaskEu();
		
		Assertions.assertNotEquals(0, regionBundleTaskEu1.hashCode());
	}
	
	@Test
	void shouldGetData() {
		RegionBundleTask regionBundleTaskEu1 = RegionTestUtil.createRegionBundleTaskEu();
		
		Assertions.assertNotNull(regionBundleTaskEu1.getData());
	}
}
