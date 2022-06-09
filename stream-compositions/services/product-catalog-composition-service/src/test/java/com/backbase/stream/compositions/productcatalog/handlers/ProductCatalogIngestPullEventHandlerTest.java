package com.backbase.stream.compositions.productcatalog.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductCatalogIngestPullEvent;
import com.backbase.stream.compositions.productcatalog.core.config.ProductCatalogConfigurationProperties;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.handlers.ProductCatalogIngestPullEventHandler;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogIngestPullEventHandlerTest {
    @Mock
    private ProductCatalogIngestionService productCatalogIngestionService;

    @Mock
    ProductCatalogMapper mapper;

    @Mock
    EventBus eventBus;

    @Test
    void testHandleEvent_Completed() {
        ProductCatalog productCatalog = new ProductCatalog();

        Mono<ProductCatalogIngestResponse> responseMono = Mono.just(
                ProductCatalogIngestResponse
                        .builder().productCatalog(productCatalog).build());

        lenient().when(productCatalogIngestionService.ingestPull(any())).thenReturn(responseMono);
        ProductCatalogConfigurationProperties properties = new ProductCatalogConfigurationProperties();

        ProductCatalogIngestPullEventHandler handler = new ProductCatalogIngestPullEventHandler(
                properties,
                productCatalogIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<ProductCatalogIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductCatalogIngestPullEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPull(any());
    }

    @Test
    void testHandleEvent_Failed() {
        when(productCatalogIngestionService.ingestPull(any())).thenThrow(new RuntimeException());
        ProductCatalogConfigurationProperties properties = new ProductCatalogConfigurationProperties();
        ProductCatalogIngestPullEventHandler handler = new ProductCatalogIngestPullEventHandler(
                properties,
                productCatalogIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<ProductCatalogIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductCatalogIngestPullEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPull(any());
    }
}
