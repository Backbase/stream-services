package com.backbase.stream.compositions.productcatalog.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCatalogIngestCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCatalogIngestFailedEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductCatalogIngestPullEvent;
import com.backbase.stream.compositions.productcatalog.core.config.ProductCatalogConfigurationProperties;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPullRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(ProductCatalogConfigurationProperties.class)
public class ProductCatalogIngestPullEventHandler implements EventHandler<ProductCatalogIngestPullEvent> {
    private final ProductCatalogConfigurationProperties configProperties;
    private final ProductCatalogIngestionService productCatalogIngestionService;
    private final ProductCatalogMapper mapper;
    private final EventBus eventBus;

    /**
     * Handles ProductCatalogIngestPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<ProductCatalogIngestPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<ProductCatalogIngestPullEvent> envelopedEvent) {
        try {
            productCatalogIngestionService
                    .ingestPull(buildRequest(envelopedEvent.getEvent()))
                    .doOnError(this::handleError)
                    .subscribe(this::handleResponse);
        } catch (Exception ex) {
            this.handleError(ex);
        }
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param productCatalogIngestPullEvent ProductCatalogIngestPullEvent
     * @return ProductCatalogIngestPullRequest
     */
    private Mono<ProductCatalogIngestPullRequest> buildRequest(ProductCatalogIngestPullEvent productCatalogIngestPullEvent) {
        return Mono.just(
                ProductCatalogIngestPullRequest.builder()
                        .additionalParameters(productCatalogIngestPullEvent.getAdditions())
                        .build());
    }

    /**
     * Handles response from ingestion service.
     *
     * @param response ProductCatalogIngestResponse
     */
    private void handleResponse(ProductCatalogIngestResponse response) {
        if (Boolean.FALSE.equals(configProperties.getEnableCompletedEvents())) {
            return;
        }
        ProductCatalogIngestCompletedEvent event = new ProductCatalogIngestCompletedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withProductCatalog(mapper.mapStreamToEvent(response.getProductCatalog()));

        EnvelopedEvent<ProductCatalogIngestCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }

    /**
     * Handles error from ingestion service.
     *
     * @param ex Throwable
     */
    private void handleError(Throwable ex) {
        if (Boolean.FALSE.equals(configProperties.getEnableFailedEvents())) {
            return;
        }
        ProductCatalogIngestFailedEvent event = new ProductCatalogIngestFailedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withMessage(ex.getMessage());

        EnvelopedEvent<ProductCatalogIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
