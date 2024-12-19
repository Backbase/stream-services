package com.backbase.stream.compositions.product.handlers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPullEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.Events;
import com.backbase.stream.compositions.product.core.mapper.EventRequestsMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.legalentity.model.ProductGroup;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductIngestPullEventHandlerTest {

    @Mock
    private ProductIngestionService productIngestionService;

    @Mock
    ProductGroupMapper mapper;

    EventRequestsMapper eventRequestsMapper = Mappers.getMapper(EventRequestsMapper.class);

    @Mock
    EventBus eventBus;

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
        ProductGroup productGroup = new ProductGroup();

        Mono<ProductIngestResponse> responseMono = Mono.just(
                ProductIngestResponse
                        .builder().productGroups(Arrays.asList(productGroup)).build());

        lenient().when(productIngestionService.ingestPull(any())).thenReturn(responseMono);
        ProductConfigurationProperties properties = new ProductConfigurationProperties();
        Events events = new Events();
        events.setEnableCompleted(isCompletedEvents);
        properties.setEvents(events);

        ProductPullEventHandler handler = new ProductPullEventHandler(
                properties,
                productIngestionService,
                mapper,
                eventBus,
                eventRequestsMapper);

        EnvelopedEvent<ProductPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new ProductPullEvent());

        handler.handle(envelopedEvent);
        verify(productIngestionService).ingestPull(any());
    }

    void testHandleEvent_Failed(Boolean isFailedEvents) {
        when(productIngestionService.ingestPull(any())).thenReturn(Mono.error(new RuntimeException()));

        ProductConfigurationProperties properties = new ProductConfigurationProperties();
        Events events = new Events();
        events.setEnableFailed(isFailedEvents);
        properties.setEvents(events);

        ProductPullEventHandler handler = new ProductPullEventHandler(
                properties,
                productIngestionService,
                mapper,
                eventBus,
                eventRequestsMapper);

        EnvelopedEvent<ProductPullEvent> envelopedEvent = new EnvelopedEvent<>();
        ProductPullEvent event = new ProductPullEvent().withLegalEntityExternalId("externalId");
        envelopedEvent.setEvent(event);
        assertThrows(RuntimeException.class, () -> handler.handle(envelopedEvent));
    }
}
