package com.backbase.stream.limit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.Entity;
import com.backbase.dbs.limit.api.service.v2.model.LimitByUuidPutResponseBody;
import com.backbase.dbs.limit.api.service.v2.model.LimitsPostResponseBody;
import com.backbase.dbs.limit.api.service.v2.model.LimitsRetrievalPostResponseBody;
import com.backbase.stream.configuration.LimitsWorkerConfigurationProperties;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LimitsSagaTest {

  @InjectMocks private LimitsSaga limitsSaga;

  @Mock private LimitsServiceApi limitsApi;

  @Mock private LimitsWorkerConfigurationProperties limitsWorkerConfigurationProperties;

  @BeforeEach
  public void init() {
    when(limitsWorkerConfigurationProperties.isEnabled()).thenReturn(Boolean.TRUE);
  }

  @ParameterizedTest
  @MethodSource("parameters_retrieveLimits_error")
  void retrieveLimits_error(Exception ex, String error) {

    // Given
    LimitsTask limitsTask = createTask();
    when(limitsApi.postLimitsRetrieval(any())).thenReturn(Flux.error(ex));

    // When
    Mono<LimitsTask> result = limitsSaga.executeTask(limitsTask);
    StreamTaskException stEx = null;
    try {
      result.block();
    } catch (StreamTaskException e) {
      stEx = e;
    }

    // Then
    verify(limitsApi).postLimitsRetrieval(any());
    Assertions.assertThat(stEx)
        .isNotNull()
        .extracting(e -> e.getTask().getHistory().get(0).getErrorMessage())
        .isEqualTo(error);
  }

  private static Stream<Arguments> parameters_retrieveLimits_error() {
    return Stream.of(
        Arguments.of(new RuntimeException("Fake error"), "Fake error"),
        Arguments.of(
            new WebClientResponseException(
                422,
                "Unprocessable Entity",
                null,
                "Fake validation error".getBytes(),
                Charset.defaultCharset()),
            "Fake validation error"));
  }

  @Test
  void createLimits() {

    // Given
    LimitsTask limitsTask = createTask();
    when(limitsApi.postLimitsRetrieval(any())).thenReturn(Flux.empty());
    when(limitsApi.postLimits(any())).thenReturn(Mono.just(new LimitsPostResponseBody()));

    // When
    Mono<LimitsTask> result = limitsSaga.executeTask(limitsTask);
    result.block();

    // Then
    verify(limitsApi).postLimitsRetrieval(any());
  }

  @Test
  void updateLimits() {

    // Given
    LimitsTask limitsTask = createTask();
    var retrieval = new LimitsRetrievalPostResponseBody();
    retrieval.uuid("uuid");
    when(limitsApi.postLimitsRetrieval(any())).thenReturn(Flux.just(retrieval));
    when(limitsApi.putLimitByUuid(any(), any()))
        .thenReturn(Mono.just(new LimitByUuidPutResponseBody()));

    // When
    Mono<LimitsTask> result = limitsSaga.executeTask(limitsTask);
    result.block();

    // Then
    verify(limitsApi).postLimitsRetrieval(any());
    verify(limitsApi).putLimitByUuid(any(), any());
  }

  private LimitsTask createTask() {
    var saEntity = new Entity().etype("SA").eref("internalSaId");
    var fagEntity = new Entity().etype("FAG").eref("internalFagId");
    var funEntity = new Entity().etype("FUN").eref("1018");
    var prvEntity = new Entity().etype("PRV").eref("approve");
    var request = new CreateLimitRequestBody();
    request.entities(List.of(saEntity, fagEntity, funEntity, prvEntity));
    request.setUserBBID("internalUserId");
    return new LimitsTask("1", request);
  }
}
