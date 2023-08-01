package com.backbase.stream.limit;

import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.backbase.dbs.limit.api.service.v2.model.*;
import com.backbase.stream.configuration.LimitsWorkerConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitsSagaTest {

    @InjectMocks
    private LimitsSaga limitsSaga;

    @Mock
    private LimitsServiceApi limitsApi;

    @Mock
    private LimitsWorkerConfigurationProperties limitsWorkerConfigurationProperties;

    @BeforeEach
    public void init() {
        when(limitsWorkerConfigurationProperties.isEnabled()).thenReturn(Boolean.TRUE);
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
        when(limitsApi.putLimitByUuid(any(), any())).thenReturn(Mono.just(new LimitByUuidPutResponseBody()));

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