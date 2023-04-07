package com.backbase.stream.compositions.productcatalog.handlers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductCatalogIngestPullEvent;
import com.backbase.stream.compositions.productcatalog.core.config.ProductCatalogConfigurationProperties;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.model.ProductCatalog;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductCatalogIngestPullEventHandlerTest {
    @Mock private ProductCatalogIngestionService productCatalogIngestionService;

    @Mock ProductCatalogMapper mapper;

    @Mock EventBus eventBus;

    @Test
    @Tag("true")
    void testEnableHandleEvent_Completed(TestInfo testInfo) {
        String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
        testHandleEvent_Completed(Boolean.valueOf(eventsConfig));
    }

    @Test
    @Tag("false")
    void testDisableHandleEvent_Completed(TestInfo testInfo) {
        String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
        testHandleEvent_Completed(Boolean.valueOf(eventsConfig));
    }

    @Test
    @Tag("true")
    void testEnableHandleEvent_Failed(TestInfo testInfo) {
        String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
        testHandleEvent_Failed(Boolean.valueOf(eventsConfig));
    }

    @Test
    @Tag("false")
    void testDisableHandleEvent_Failed(TestInfo testInfo) {
        String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
        testHandleEvent_Failed(Boolean.valueOf(eventsConfig));
    }

    void testHandleEvent_Completed(Boolean isCompletedEvents) {
        ProductCatalog productCatalog = new ProductCatalog();

        Mono<ProductCatalogIngestResponse> responseMono =
                Mono.just(
                        ProductCatalogIngestResponse.builder()
                                .productCatalog(productCatalog)
                                .build());

        lenient().when(productCatalogIngestionService.ingestPull(any())).thenReturn(responseMono);
        ProductCatalogConfigurationProperties properties =
                new ProductCatalogConfigurationProperties();
        properties.setEnableCompletedEvents(isCompletedEvents);
        ProductCatalogIngestPullEventHandler handler =
                new ProductCatalogIngestPullEventHandler(
                        properties, productCatalogIngestionService, mapper, eventBus);

        EnvelopedEvent<ProductCatalogIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductCatalogIngestPullEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPull(any());
    }

    void testHandleEvent_Failed(Boolean isFailedEvents) {
        when(productCatalogIngestionService.ingestPull(any())).thenThrow(new RuntimeException());
        ProductCatalogConfigurationProperties properties =
                new ProductCatalogConfigurationProperties();
        properties.setEnableFailedEvents(isFailedEvents);
        ProductCatalogIngestPullEventHandler handler =
                new ProductCatalogIngestPullEventHandler(
                        properties, productCatalogIngestionService, mapper, eventBus);

        EnvelopedEvent<ProductCatalogIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductCatalogIngestPullEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPull(any());
    }
}
