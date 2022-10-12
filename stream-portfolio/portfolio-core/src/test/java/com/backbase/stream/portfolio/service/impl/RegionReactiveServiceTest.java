package com.backbase.stream.portfolio.service.impl;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.saga.region.RegionBundleSaga;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * RegionReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class RegionReactiveServiceTest {

	@Mock
	private PortfolioSagaProperties portfolioSagaProperties;

	@Mock
	private RegionBundleSaga regionBundleSaga;

	@InjectMocks
	private RegionReactiveService regionReactiveService;

	@Test
	void shouldIngestRegionBundles() {
		RegionBundle regionBundle = new RegionBundle();

		Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
		Mockito.when(regionBundleSaga.executeTask(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

		Flux<RegionBundle> regionBundles = regionReactiveService
				.ingestRegionBundles(Flux.fromIterable(List.of(regionBundle)));

		Assertions.assertNotNull(regionBundles);

		StepVerifier.create(regionBundles).assertNext(assertEqualsTo(regionBundle)).verifyComplete();
	}
}
