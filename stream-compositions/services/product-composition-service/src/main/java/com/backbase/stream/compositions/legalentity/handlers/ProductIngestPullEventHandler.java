package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductsIngestCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductsIngestFailedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductsIngestPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.ProductIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(ProductConfigurationProperties.class)
public class ProductIngestPullEventHandler implements EventHandler<ProductsIngestPullEvent> {
    private final ProductConfigurationProperties configProperties;
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper mapper;
    private final EventBus eventBus;

    /**
     * Handles ProductsIngestPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<ProductsIngestPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<ProductsIngestPullEvent> envelopedEvent) {
        productIngestionService
                .ingestPull(buildRequest(envelopedEvent.getEvent()))
                .doOnError(this::handleError)
                .subscribe(this::handleResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param event ProductsIngestPullEvent
     * @return ProductIngestPullRequest
     */
    private ProductIngestPullRequest buildRequest(ProductsIngestPullEvent event) {
        return ProductIngestPullRequest.builder()
                .legalEntityExternalId(event.getLegalEntityExternalId())
                .build();
    }

    /**
     * Handles reponse from ingestion service.
     *
     * @param response ProductIngestResponse
     */
    private void handleResponse(ProductIngestResponse response) {
        if (configProperties.getEnableCompletedEvents() == false) {
            return;
        }
        ProductsIngestCompletedEvent event = new ProductsIngestCompletedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withProductGroup(mapper.mapStreamToEvent(response.getProductGroup()));

        EnvelopedEvent<ProductsIngestCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }

    /**
     * Handles error from ingestion service.
     *
     * @param ex Throwable
     */
    private void handleError(Throwable ex) {
        if (configProperties.getEnableFailedEvents() == false) {
            return;
        }
        ProductsIngestFailedEvent event = new ProductsIngestFailedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withMessage(ex.getMessage());

        EnvelopedEvent<ProductsIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
