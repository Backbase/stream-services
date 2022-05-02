package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.LegalEntityCompositionApplication;
import com.backbase.stream.compositions.legalentity.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.service.impl.RootLegalEntityBootstrapTask;
import com.backbase.stream.legalentity.model.LegalEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Import(LegalEntityCompositionApplication.class)
class RootLegalEntityBootstrapTaskTest {
    @Mock
    LegalEntitySaga legalEntitySaga;

    @Mock
    LegalEntityTask legalEntityTask;

    @Mock
    EventBus eventBus;

    @Test
    void testRootIngestion() {
        LegalEntity legalEntity = new LegalEntity().name("Test Legal Entity").externalId("externalId");

        lenient().when(legalEntityTask.getLegalEntity()).thenReturn(legalEntity);
        lenient().when(legalEntityTask.getData()).thenReturn(legalEntity);

        lenient().when(legalEntitySaga.executeTask(any()))
                .thenReturn(Mono.just(legalEntityTask));

        BootstrapConfigurationProperties bootstrapConfigurationProperties = new BootstrapConfigurationProperties();
        bootstrapConfigurationProperties.setLegalEntity(new LegalEntity().name("Test Legal Entity").externalId("externalId"));

        RootLegalEntityBootstrapTask bootstrapTask = new RootLegalEntityBootstrapTask(
                legalEntitySaga,
                bootstrapConfigurationProperties,
                eventBus);

        bootstrapTask.run(null);
        verify(legalEntitySaga).executeTask(any());
    }
}
