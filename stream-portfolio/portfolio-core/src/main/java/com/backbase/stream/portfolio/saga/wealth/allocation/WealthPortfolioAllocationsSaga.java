package com.backbase.stream.portfolio.saga.wealth.allocation;

import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;

import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.worker.StreamTaskExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WealthPortfolioAllocations Saga.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthPortfolioAllocationsSaga implements StreamTaskExecutor<WealthPortfolioAllocationsTask> {
	private static final String UPSERT = "upsert";
	private static final String PORTFOLIO_ALLOCATIONS_ENTITY = "PORTFOLIO_ALLOCATIONS_ENTITY";
	private static final String UPSERT_PORTFOLIO_ALLOCATIONS = "upsert-portfolio-allocations";

	private final PortfolioIntegrationService portfolioIntegrationService;

	@Override
	public Mono<WealthPortfolioAllocationsTask> executeTask(WealthPortfolioAllocationsTask streamTask) {
		return upsertPortfolioAllocations(streamTask);
	}

	@Override
	public Mono<WealthPortfolioAllocationsTask> rollBack(WealthPortfolioAllocationsTask streamTask) {
		return Mono.just(streamTask);
	}

	@ContinueSpan(log = UPSERT_PORTFOLIO_ALLOCATIONS)
	private Mono<WealthPortfolioAllocationsTask> upsertPortfolioAllocations(
			@SpanTag(value = "streamTask") WealthPortfolioAllocationsTask task) {
		task.info(PORTFOLIO_ALLOCATIONS_ENTITY, UPSERT, null, null, null, "Upsert Portfolio Allocations");

		log.info("Upserting portfolio allocation", task.getName());

		AllocationBundle allocationBundle = task.getData();

		return portfolioIntegrationService
				.upsertAllocations(allocationBundle.getAllocations(), allocationBundle.getPortfolioCode())
				.map(o -> task);
	}
}
