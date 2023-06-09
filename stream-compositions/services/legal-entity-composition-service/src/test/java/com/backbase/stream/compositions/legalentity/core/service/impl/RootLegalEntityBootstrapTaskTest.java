package com.backbase.stream.compositions.legalentity.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.LegalEntityCompositionApplication;
import com.backbase.stream.compositions.legalentity.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.legalentity.model.LegalEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@Import(LegalEntityCompositionApplication.class)
class RootLegalEntityBootstrapTaskTest {

  @Mock LegalEntitySaga legalEntitySaga;

  @Mock LegalEntityTask legalEntityTask;

  @Test
  void testRootIngestion_Success() {
    LegalEntity legalEntity =
        new LegalEntity()
            .name("Test Legal Entity")
            .externalId("externalId")
            .internalId("internalId");

    lenient().when(legalEntityTask.getLegalEntity()).thenReturn(legalEntity);
    lenient().when(legalEntityTask.getData()).thenReturn(legalEntity);

    lenient().when(legalEntitySaga.executeTask(any())).thenReturn(Mono.just(legalEntityTask));

    BootstrapConfigurationProperties bootstrapConfigurationProperties =
        new BootstrapConfigurationProperties();
    bootstrapConfigurationProperties.setLegalEntity(
        new LegalEntity().name("Test Legal Entity").externalId("externalId"));
    bootstrapConfigurationProperties.setEnabled(Boolean.TRUE);

    RootLegalEntityBootstrapTask bootstrapTask =
        new RootLegalEntityBootstrapTask(legalEntitySaga, bootstrapConfigurationProperties);

    bootstrapTask.run(null);
    verify(legalEntitySaga, times(1)).executeTask(any());
    assertNotNull(legalEntitySaga.executeTask(any()).block().getData().getInternalId());
    assertTrue(bootstrapConfigurationProperties.getEnabled());
  }

  @Test
  void testRootIngestion_Fail() {

    BootstrapConfigurationProperties bootstrapConfigurationProperties =
        new BootstrapConfigurationProperties();
    bootstrapConfigurationProperties.setLegalEntity(null);
    bootstrapConfigurationProperties.setEnabled(Boolean.TRUE);

    RootLegalEntityBootstrapTask bootstrapTask =
        new RootLegalEntityBootstrapTask(legalEntitySaga, bootstrapConfigurationProperties);

    bootstrapTask.run(null);
    verify(legalEntitySaga, times(0)).executeTask(any());
  }
}
