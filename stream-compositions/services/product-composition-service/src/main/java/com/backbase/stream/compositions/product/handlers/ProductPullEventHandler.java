package com.backbase.stream.compositions.product.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductFailedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPullEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
@EnableConfigurationProperties(ProductConfigurationProperties.class)
public class ProductPullEventHandler implements EventHandler<ProductPullEvent> {
    private final ProductConfigurationProperties configProperties;
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper mapper;
    private final EventBus eventBus;

    /**
     * Handles ProductsPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<ProductsPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<ProductPullEvent> envelopedEvent) {
        buildRequest(envelopedEvent.getEvent())
                .flatMap(productIngestionService::ingestPull)
                .doOnError(this::handleError)
                .subscribe(this::handleResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param event ProductsPullEvent
     * @return ProductPullRequest
     */
    private Mono<ProductIngestPullRequest> buildRequest(ProductPullEvent event) {
        return Mono.just(
                ProductIngestPullRequest.builder()
                        .legalEntityExternalId(event.getLegalEntityExternalId())
                        .build());
    }

    /**
     * Handles reponse from ingestion service.
     *
     * @param response ProductIngestResponse
     */
    private void handleResponse(ProductIngestResponse response) {
        if (Boolean.FALSE.equals(configProperties.getEvents().getEnableCompleted())) {
            return;
        }
        ProductCompletedEvent event = new ProductCompletedEvent()
                .withProductGroups(
                        response.getProductGroups().stream()
                                .map( productGroup -> mapper.mapStreamToEvent(productGroup))
                                .collect(Collectors.toList()));

        EnvelopedEvent<ProductCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }

    /**
     * Handles error from ingestion service.
     *
     * @param ex Throwable
     */
    private void handleError(Throwable ex) {
        log.error("Error ingesting legal entity using the Pull event: {}", ex.getMessage());

        if (Boolean.TRUE.equals(configProperties.getEvents().getEnableFailed())) {
            ProductFailedEvent event = new ProductFailedEvent()
                    .withMessage(ex.getMessage());

            EnvelopedEvent<ProductFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
    }
}
