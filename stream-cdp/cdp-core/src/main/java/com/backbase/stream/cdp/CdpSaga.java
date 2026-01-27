package com.backbase.stream.cdp;

import com.backbase.cdp.ingestion.api.service.v1.CdpApi;
import com.backbase.stream.configuration.CdpProperties;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class CdpSaga implements StreamTaskExecutor<CdpTask> {

    public static final String ENTITY = "CustomerOnboarded";
    public static final String INGEST = "ingest";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String INGESTED_SUCCESSFULLY = "Customer ingested successfully";
    public static final String FAILED_TO_INGEST = "Failed to ingest Customer";

    private final CdpApi cdpServiceApi;
    private final CdpProperties cdpProperties;

    public CdpSaga(
        CdpApi cdpServiceApi,
        CdpProperties cdpProperties
    ) {
        this.cdpServiceApi = cdpServiceApi;
        this.cdpProperties = cdpProperties;
    }

    @Override
    public Mono<CdpTask> executeTask(CdpTask streamTask) {

        var request = streamTask.getCdpEvents();

        return cdpServiceApi.ingestEvents(request)
            .then(Mono.fromCallable(() -> {
                streamTask.info(ENTITY, INGEST, SUCCESS, null, null, INGESTED_SUCCESSFULLY);
                return streamTask;
            }))
            .onErrorResume(throwable -> {
                streamTask.error(ENTITY, INGEST, ERROR, null, null, FAILED_TO_INGEST);
                return Mono.error(new StreamTaskException(streamTask, throwable, FAILED_TO_INGEST));
            });
    }

    @Override
    public Mono<CdpTask> rollBack(CdpTask streamTask) {
        return null;
    }

    public boolean isEnabled() {
        if (cdpProperties == null) {
            return false;
        }

        return cdpProperties.enabled();
    }

    public String getDefaultCustomerCategory() {
        if (!isEnabled()) {
            return null;
        }

        return cdpProperties.defaultCustomerCategory();
    }
}
