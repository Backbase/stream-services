package com.backbase.stream.audiences;

import com.backbase.audiences.collector.api.service.v1.HandlersServiceApi;
import com.backbase.stream.configuration.UserKindSegmentationProperties;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class UserKindSegmentationSaga implements StreamTaskExecutor<UserKindSegmentationTask> {

  public static final String ENTITY = "CustomerOnboarded";
  public static final String INGEST = "ingest";
  public static final String SUCCESS = "success";
  public static final String ERROR = "error";
  public static final String INGESTED_SUCCESSFULLY = "Customer ingested successfully";
  public static final String FAILED_TO_INGEST = "Failed to ingest Customer";

  private final HandlersServiceApi handlersServiceApi;
  private final UserKindSegmentationProperties userKindSegmentationProperties;

  public UserKindSegmentationSaga(
      HandlersServiceApi handlersServiceApi,
      UserKindSegmentationProperties userKindSegmentationProperties) {
    this.handlersServiceApi = handlersServiceApi;
    this.userKindSegmentationProperties = userKindSegmentationProperties;
  }

  @Override
  public Mono<UserKindSegmentationTask> executeTask(UserKindSegmentationTask streamTask) {
    var request = streamTask.getCustomerOnboardedRequest();

    return handlersServiceApi
        .customerOnboarded(request)
        .then(
            Mono.fromCallable(
                () -> {
                  streamTask.info(
                      ENTITY,
                      INGEST,
                      SUCCESS,
                      null,
                      request.getInternalUserId(),
                      INGESTED_SUCCESSFULLY);
                  return streamTask;
                }))
        .onErrorResume(
            throwable -> {
              streamTask.error(
                  ENTITY, INGEST, ERROR, null, request.getInternalUserId(), FAILED_TO_INGEST);
              return Mono.error(new StreamTaskException(streamTask, throwable, FAILED_TO_INGEST));
            });
  }

  @Override
  public Mono<UserKindSegmentationTask> rollBack(UserKindSegmentationTask streamTask) {
    return null;
  }

  public boolean isEnabled() {
    if (userKindSegmentationProperties == null) {
      return false;
    }

    return userKindSegmentationProperties.enabled();
  }

  public String getDefaultCustomerCategory() {
    if (!isEnabled()) {
      return null;
    }

    return userKindSegmentationProperties.defaultCustomerCategory();
  }
}
