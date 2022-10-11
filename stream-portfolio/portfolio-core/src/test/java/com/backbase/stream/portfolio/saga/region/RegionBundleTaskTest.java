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
		RegionBundleTask regionBundleTask = RegionTestUtil.createRegionBundleTask();

		Assertions.assertNotNull(regionBundleTask.getName());

		RegionBundle data = regionBundleTask.getData();

		Assertions.assertNotNull(data);
	}
}
