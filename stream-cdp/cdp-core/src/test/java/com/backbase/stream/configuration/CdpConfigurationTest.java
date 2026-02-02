package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.cdp.ingestion.api.service.v1.CdpApi;
import com.backbase.stream.cdp.CdpSaga;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CdpConfigurationTest {

    @Test
    void testCdpSagaBeanCreation() {
        // Arrange: create mocks
        CdpApi cdpApi = Mockito.mock(CdpApi.class);
        CdpProperties cdpProperties = new CdpProperties(true, "RETAIL");

        // Use a test configuration to inject mocks
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(CdpApi.class, () -> cdpApi);
        context.registerBean(CdpProperties.class, () -> cdpProperties);
        context.register(CdpConfiguration.class);
        context.refresh();

        // Act: get the bean
        CdpSaga cdpSaga = context.getBean(CdpSaga.class);

        // Assert
        assertThat(cdpSaga).isNotNull();
        assertThat(cdpSaga).isInstanceOf(CdpSaga.class);
        context.close();
    }

    @Test
    void testCdpSagaBeanCreation_withRecordProperties() {
        // Arrange: create mocks
        CdpApi cdpApi = Mockito.mock(CdpApi.class);
        // Provide required constructor args for record
        CdpProperties cdpProperties = new CdpProperties(false, "BUSINESS");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(CdpApi.class, () -> cdpApi);
        context.registerBean(CdpProperties.class, () -> cdpProperties);
        context.register(CdpConfiguration.class);
        context.refresh();

        CdpSaga cdpSaga = context.getBean(CdpSaga.class);
        assertThat(cdpSaga).isNotNull();
        assertThat(cdpSaga).isInstanceOf(CdpSaga.class);
        context.close();
    }
}
