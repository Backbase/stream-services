package com.backbase.stream.portfolio.saga.wealth.region;

import java.util.List;

import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;

import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.worker.StreamTaskExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WealthRegions Saga.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthRegionsSaga implements StreamTaskExecutor<WealthRegionsTask> {

	private static final String UPSERT = "upsert";
	private static final String REGION_ENTITY = "REGION_ENTITY";
	private static final String UPSERT_REGIONS = "upsert-regions";

	private final InstrumentIntegrationService instrumentIntegrationService;

	@Override
	public Mono<WealthRegionsTask> executeTask(WealthRegionsTask streamTask) {
		return upsertRegions(streamTask);
	}

	@Override
	public Mono<WealthRegionsTask> rollBack(WealthRegionsTask streamTask) {
		return Mono.just(streamTask);
	}

	@ContinueSpan(log = UPSERT_REGIONS)
	private Mono<WealthRegionsTask> upsertRegions(@SpanTag(value = "streamTask") WealthRegionsTask task) {
		task.info(REGION_ENTITY, UPSERT, null, null, null, "Upsert Regions");

		log.info("Upserting region", task.getName());

		return instrumentIntegrationService.upsertRegions(List.of(task.getData())).map(o -> task);
	}

}
