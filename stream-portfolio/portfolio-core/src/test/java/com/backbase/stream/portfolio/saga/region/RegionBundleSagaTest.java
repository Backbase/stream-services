package com.backbase.stream.portfolio.saga.region;

import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.util.RegionTestUtil;

import reactor.core.publisher.Mono;

/**
 * RegionBundleSaga Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class RegionBundleSagaTest {

	@Mock
	private InstrumentIntegrationService instrumentIntegrationService;

	@InjectMocks
	private RegionBundleSaga regionBundleSaga;

	@Test
	void shouldExecuteTask() {
		RegionBundle regionBundleEu = RegionTestUtil.createRegionBundleEu();
		RegionBundleTask regionBundleTask = new RegionBundleTask(regionBundleEu);
		List<RegionBundle> regionBundles = List.of(regionBundleEu);

		Mockito.when(instrumentIntegrationService.upsertRegions(regionBundles)).thenReturn(Mono.just(regionBundles));

		Mono<RegionBundleTask> task = regionBundleSaga.executeTask(regionBundleTask);

		Assertions.assertNotNull(task);

		task.block();

		Mockito.verify(instrumentIntegrationService, times(1)).upsertRegions(regionBundles);
	}

	@Test
	void shouldRollBack() {
		RegionBundleTask regionBundleTask = RegionTestUtil.createRegionBundleTaskEu();

		Mono<RegionBundleTask> mono = regionBundleSaga.rollBack(regionBundleTask);

		Assertions.assertNotNull(mono);

		Mockito.verify(instrumentIntegrationService, times(0)).upsertRegions(ArgumentMatchers.anyList());
	}
}
