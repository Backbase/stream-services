package com.backbase.stream.limit;

import static org.springframework.util.CollectionUtils.isEmpty;

import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.LimitsRetrievalPostResponseBody;
import com.backbase.stream.configuration.LimitsWorkerConfigurationProperties;
import com.backbase.stream.mapper.LimitsMapper;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class LimitsSaga implements StreamTaskExecutor<LimitsTask> {

    public static final String LIMIT = "limit";
    public static final String RETRIEVE = "retrieve";
    public static final String CREATE = "create";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String SPACE = " ";
    public static final String CREATED_SUCCESSFULLY = "Limit created successfully";
    public static final String UPDATED_SUCCESSFULLY = "Limit updated successfully";
    public static final String FAILED_TO_INGEST_LIMITS = "Failed to ingest limits";
    private final LimitsServiceApi limitsApi;

    private final LimitsWorkerConfigurationProperties limitsWorkerConfigurationProperties;
    private final LimitsMapper mapper = Mappers.getMapper(LimitsMapper.class);

    @Override
    public Mono<LimitsTask> executeTask(LimitsTask limitsTask) {
        CreateLimitRequestBody item = limitsTask.getData();

        if (!limitsWorkerConfigurationProperties.isEnabled()) {
            log.info("backbase.stream.limits.worker.enabled is false, Skipping limits ingestion");
            return Mono.just(limitsTask);
        }

        log.info("Started ingestion of limits {} for user {}",
                item.getEntities().stream().map(entity -> entity.getEtype() + COLON + SPACE + entity.getEref())
                        .collect(Collectors.joining(COMMA + SPACE)), item.getUserBBID());
        return limitsApi.postLimitsRetrieval(mapper.map(item))
            .onErrorResume(throwable -> {
                if (throwable instanceof WebClientResponseException webClientResponseException) {
                    limitsTask.error(LIMIT, RETRIEVE, ERROR, item.getUserBBID(), null, webClientResponseException,
                        webClientResponseException.getResponseBodyAsString(), FAILED_TO_INGEST_LIMITS);
                } else {
                    limitsTask.error(LIMIT, RETRIEVE, ERROR, item.getUserBBID(), null, throwable,
                        throwable.getMessage(), FAILED_TO_INGEST_LIMITS);
                }
                return Mono.error(new StreamTaskException(limitsTask, throwable, FAILED_TO_INGEST_LIMITS));
            })
            .collectList()
            .flatMap(limitsRetrievalPostResponseBody -> {
                if (isEmpty(limitsRetrievalPostResponseBody)) {
                    log.info("Creating Limits");
                    return createLimits(limitsTask, item);
                } else {
                    log.info("Updating Limits");
                    return updateLimits(limitsTask, item, limitsRetrievalPostResponseBody);
                }
            });
    }

    private Mono<? extends LimitsTask> updateLimits(LimitsTask limitsTask, CreateLimitRequestBody item,
        List<LimitsRetrievalPostResponseBody> limitsRetrievalPostResponseBody) {

        var uuid = limitsRetrievalPostResponseBody.stream().filter(res -> Objects.nonNull(res.getUuid())).findFirst()
            .orElseThrow().getUuid();

        return limitsApi.putLimitByUuid(uuid, mapper.mapUpdateLimits(item))
            .map(responseBody -> {
                limitsTask.info(LIMIT, CREATE, SUCCESS, item.getUserBBID(), responseBody.getUuid(),
                    UPDATED_SUCCESSFULLY);
                return limitsTask;
            })
            .onErrorResume(throwable -> {
                limitsTask.error(LIMIT, CREATE, ERROR, item.getUserBBID(), null, throwable,
                    "Failed to ingest limit " + throwable.getMessage(), FAILED_TO_INGEST_LIMITS);
                return Mono.error(new StreamTaskException(limitsTask, throwable, FAILED_TO_INGEST_LIMITS));
            });
    }

    private Mono<LimitsTask> createLimits(LimitsTask limitsTask, CreateLimitRequestBody item) {
        return limitsApi.postLimits(item)
            .map(createLimitResponse -> {
                limitsTask.setResponse(createLimitResponse);
                limitsTask.info(LIMIT, CREATE, SUCCESS, item.getUserBBID(), createLimitResponse.getUuid(),
                    CREATED_SUCCESSFULLY);
                return limitsTask;
            })
            .onErrorResume(throwable -> {
                limitsTask.error(LIMIT, CREATE, ERROR, item.getUserBBID(), null, throwable,
                    "Failed to ingest limit " + throwable.getMessage(), FAILED_TO_INGEST_LIMITS);
                return Mono.error(new StreamTaskException(limitsTask, throwable, FAILED_TO_INGEST_LIMITS));
            });
    }

    @Override
    public Mono<LimitsTask> rollBack(LimitsTask limitsTask) {
        return null;
    }
}
