package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductsIngestPushEvent;
import com.backbase.stream.compositions.legalentity.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.service.ProductIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ProductIngestPushEventHandler implements EventHandler<ProductsIngestPushEvent> {
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper mapper;

    @Override
    public void handle(EnvelopedEvent<ProductsIngestPushEvent> envelopedEvent) {
        productIngestionService.ingestPush(buildRequest(envelopedEvent));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param envelopedEvent EnvelopedEvent<ProductsIngestPushEvent>
     * @return ProductIngestPushRequest
     */
    private Mono<ProductIngestPushRequest> buildRequest(EnvelopedEvent<ProductsIngestPushEvent> envelopedEvent) {
        return Mono.just(
                ProductIngestPushRequest.builder()
                        .productGroup(mapper.mapEventToStream(envelopedEvent.getEvent().getProductGroup()))
                        .build());
    }
}
