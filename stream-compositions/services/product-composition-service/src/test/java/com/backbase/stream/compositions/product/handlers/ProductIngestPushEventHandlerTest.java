package com.backbase.stream.compositions.product.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPushEvent;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.legalentity.model.ProductGroup;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

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
                        .builder().productGroups(Arrays.asList(productGroup)).build());

        lenient().when(productCatalogIngestionService.ingestPush(any())).thenReturn(responseMono);

        ProductPushEventHandler handler = new ProductPushEventHandler(
                productCatalogIngestionService,
                mapper);

        EnvelopedEvent<ProductPushEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductPushEvent());

        handler.handle(envelopedEvent);
        verify(productCatalogIngestionService).ingestPush(any());
    }
}
