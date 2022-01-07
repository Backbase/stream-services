package com.backbase.stream.compositions.product.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductsIngestPullEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.legalentity.model.ProductGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductIngestPullEventHandlerTest {
    @Mock
    private ProductIngestionService productIngestionService;

    @Mock
    ProductGroupMapper mapper;

    @Mock
    EventBus eventBus;

    @Test
    void testHandleEvent_Completed() {
        ProductGroup productGroup = new ProductGroup();

        Mono<ProductIngestResponse> responseMono = Mono.just(
                ProductIngestResponse
                        .builder().productGroup(productGroup).build());

        lenient().when(productIngestionService.ingestPull(any())).thenReturn(responseMono);
        ProductConfigurationProperties properties = new ProductConfigurationProperties();

        ProductIngestPullEventHandler handler = new ProductIngestPullEventHandler(
                properties,
                productIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<ProductsIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductsIngestPullEvent());

        handler.handle(envelopedEvent);
        verify(productIngestionService).ingestPull(any());
    }

    @Test
    void testHandleEvent_Failed() {
        when(productIngestionService.ingestPull(any())).thenThrow(new RuntimeException());

        ProductConfigurationProperties properties = new ProductConfigurationProperties();

        ProductIngestPullEventHandler handler = new ProductIngestPullEventHandler(
                properties,
                productIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<ProductsIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        ProductsIngestPullEvent event = new ProductsIngestPullEvent().withLegalEntityExternalId("externalId");
        envelopedEvent.setEvent(event);

        assertThrows(RuntimeException.class, () -> {
            handler.handle(envelopedEvent);
        });
    }
}
