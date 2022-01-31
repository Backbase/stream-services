package com.backbase.stream.compositions.product.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductsIngestPushEvent;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.legalentity.model.ProductGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductIngestPushEventHandlerTest {
    @Mock
    private ProductIngestionService productCatalogIngestionService;

    @Mock
    ProductGroupMapper mapper;

    @Test
    void testHandleEvent_Completed() {
        ProductGroup productGroup = new ProductGroup();

        Mono<ProductIngestResponse> responseMono = Mono.just(
                ProductIngestResponse
                        .builder().productGroup(productGroup).build());

        lenient().when(productCatalogIngestionService.ingestPush(any())).thenReturn(responseMono);

        ProductIngestPushEventHandler handler = new ProductIngestPushEventHandler(
                productCatalogIngestionService,
                mapper);

        EnvelopedEvent<ProductsIngestPushEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductsIngestPushEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPush(any());
    }
}
