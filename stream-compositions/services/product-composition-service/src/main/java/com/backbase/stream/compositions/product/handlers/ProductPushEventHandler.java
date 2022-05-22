package com.backbase.stream.compositions.product.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPushEvent;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ProductPushEventHandler implements EventHandler<ProductPushEvent> {
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper mapper;

    @Override
    public void handle(EnvelopedEvent<ProductPushEvent> envelopedEvent) {
        buildRequest(envelopedEvent)
                .flatMap(productIngestionService::ingestPush)
                .subscribe();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param envelopedEvent EnvelopedEvent<ProductsIngestPushEvent>
     * @return ProductIngestPushRequest
     */
    private Mono<ProductIngestPushRequest> buildRequest(EnvelopedEvent<ProductPushEvent> envelopedEvent) {
        return Mono.just(
                ProductIngestPushRequest.builder()
                        .productGroup(mapper.mapEventToStream(envelopedEvent.getEvent().getProductGroup()))
                        .build());
    }
}
