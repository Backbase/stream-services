package com.backbase.stream.compositions.productcatalog.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductCatalogIngestPushEvent;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.model.ProductCatalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductCatalogIngestPushEventHandlerTest {
    @Mock private ProductCatalogIngestionService productCatalogIngestionService;

    @Mock ProductCatalogMapper mapper;

    @Test
    void testHandleEvent_Completed() {
        ProductCatalog productCatalog = new ProductCatalog();

        Mono<ProductCatalogIngestResponse> responseMono =
                Mono.just(
                        ProductCatalogIngestResponse.builder()
                                .productCatalog(productCatalog)
                                .build());

        lenient().when(productCatalogIngestionService.ingestPush(any())).thenReturn(responseMono);

        ProductCatalogIngestPushEventHandler handler =
                new ProductCatalogIngestPushEventHandler(productCatalogIngestionService, mapper);

        EnvelopedEvent<ProductCatalogIngestPushEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductCatalogIngestPushEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPush(any());
    }
}
