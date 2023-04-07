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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LimitsSagaTest {

  @InjectMocks private LimitsSaga limitsSaga;

  @Mock private LimitsServiceApi limitsApi;

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
    verify(limitsApi).postLimits(any());
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
