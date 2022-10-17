package com.backbase.stream.portfolio.saga.wealth.asset;

import java.util.List;
import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * WealthAssets Saga.
 * 
 * @author Vladimir Kirchev
 *
 */
@RequiredArgsConstructor
public class WealthAssetsSaga implements StreamTaskExecutor<WealthAssetsTask> {
    private static final String ASSET_CLASS_ENTITY = "ASSET_CLASS_ENTITY";
    private static final String UPSERT_ASSET_CLASSES = "upsert-asset-classes";
    private static final String UPSERT = "upsert";

    private final InstrumentIntegrationService instrumentIntegrationService;

    @Override
    public Mono<WealthAssetsTask> executeTask(WealthAssetsTask streamTask) {
        return upsertAssetClasses(streamTask);
    }

    @Override
    public Mono<WealthAssetsTask> rollBack(WealthAssetsTask streamTask) {
        return Mono.just(streamTask);
    }

    @ContinueSpan(log = UPSERT_ASSET_CLASSES)
    private Mono<WealthAssetsTask> upsertAssetClasses(@SpanTag(value = "streamTask") WealthAssetsTask task) {
        task.info(ASSET_CLASS_ENTITY, UPSERT, null, null, null, "Upsert asset classes");
        return instrumentIntegrationService.upsertAssetClass(List.of(task.getData())).map(o -> task);
    }

}
