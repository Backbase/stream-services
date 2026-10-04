package com.backbase.stream.cdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.cdp.ingestion.api.service.v1.CdpApi;
import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvent;
import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvents;
import com.backbase.stream.configuration.CdpProperties;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CdpSagaTest {

    @Mock
    private CdpApi cdpServiceApi;

    @Mock
    private CdpProperties userKindSegmentationProperties;

    @InjectMocks
    private CdpSaga cdpSaga;

    @Test
    void testExecuteTask() {
        var task = createTask();
        when(cdpServiceApi.ingestEvents(any())).thenReturn(Mono.empty());

        cdpSaga.executeTask(task).block();

        verify(cdpServiceApi).ingestEvents(any());
    }

    @Test
    void testExecuteTaskFailure() {
        var task = createTask();
        when(cdpServiceApi.ingestEvents(any()))
            .thenReturn(Mono.error(new RuntimeException("Ingestion failed")));

        try {
            cdpSaga.executeTask(task).block();
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("Ingestion failed");
        }

        verify(cdpServiceApi).ingestEvents(any());
    }

    @Test
    void isDisabledByDefault() {
        var saga = new CdpSaga(
            cdpServiceApi,
            null
        );

        assertThat(saga.isEnabled()).isFalse();
    }

    @Test
    void defaultCustomerCategoryIsNullWhenSagaIsDisabled() {
        when(userKindSegmentationProperties.enabled()).thenReturn(false);

        assertThat(cdpSaga.getDefaultCustomerCategory()).isNull();
    }

    @Test
    void rollbackReturnsNull() {
        assertThat(cdpSaga.rollBack(createTask())).isNull();
    }

    @Test
    void returnsDefaultCustomerCategoryFromProperties() {
        when(userKindSegmentationProperties.enabled()).thenReturn(true);
        when(userKindSegmentationProperties.defaultCustomerCategory()).thenReturn("RETAIL");

        assertThat(cdpSaga.getDefaultCustomerCategory()).isEqualTo("RETAIL");
    }

    private CdpTask createTask() {
        var task = new CdpTask();
        task.setCdpEvents(
            new CdpEvents()
                .addCdpEventsItem(new CdpEvent()
                    .eventType("ProfileCreatedEvent")
                    .eventId(UUID.randomUUID().toString())
                    .sourceSystem("BACKBASE")
                    .sourceType("USER_ID")
                    .sourceId("internal-id")
                    .data(Map.of()))
        );
        return task;
    }
}