package com.backbase.stream.limit;

import com.backbase.dbs.limit.service.api.LimitsApi;
import com.backbase.dbs.limit.service.model.CreateLimitRequest;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class LimitsSaga implements StreamTaskExecutor<LimitsTask> {

    public static final String LIMIT = "limit";
    public static final String CREATE = "create";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String CREATED_SUCCESSFULLY = "Limit created successfully";
    public static final String FAILED_TO_INGEST_LIMITS = "Failed to ingest limits";
    private LimitsApi limitsApi;

    public LimitsSaga(LimitsApi limitsApi) {
        this.limitsApi = limitsApi;
    }

    @Override
    public Mono<LimitsTask> executeTask(LimitsTask limitsTask) {
        CreateLimitRequest item = limitsTask.getData();


        log.info("Started ingestion of transactions for user {}", item.getUserBBID());
        return limitsApi.postLimitsService(item)
                .map(createLimitResponse -> {
                    limitsTask.setResponse(createLimitResponse);
                    limitsTask.info(LIMIT, CREATE, SUCCESS, item.getUserBBID(), createLimitResponse.getUuid(), CREATED_SUCCESSFULLY);
                    return limitsTask;
                })
                .onErrorResume(throwable -> {
                    limitsTask.error(LIMIT, CREATE, ERROR, item.getUserBBID(), null, throwable, "Failed to ingest limit " + throwable.getMessage(), FAILED_TO_INGEST_LIMITS);
                    return Mono.error(new StreamTaskException(limitsTask, throwable, FAILED_TO_INGEST_LIMITS));
                });

    }

    @Override
    public Mono<LimitsTask> rollBack(LimitsTask limitsTask) {
        return null;
    }
}
