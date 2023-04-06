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
import com.backbase.stream.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.WealthBundle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PortfolioReactiveService Test.
 *
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class PortfolioReactiveServiceTest {

	@Mock
	private PortfolioSagaProperties portfolioSagaProperties;

	@Mock
	private PortfolioSaga portfolioSaga;

	@InjectMocks
	private PortfolioReactiveService portfolioReactiveService;

	@Test
	void shouldIngestWealthBundles() {
		WealthBundle wealthBundle = new WealthBundle();

		Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
		Mockito.when(portfolioSaga.executeTask(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

		Flux<WealthBundle> wealthBundles = portfolioReactiveService
				.ingestWealthBundles(Flux.fromIterable(List.of(wealthBundle)));

		Assertions.assertNotNull(wealthBundles);

		StepVerifier.create(wealthBundles).assertNext(assertEqualsTo(wealthBundle)).verifyComplete();
	}
}
