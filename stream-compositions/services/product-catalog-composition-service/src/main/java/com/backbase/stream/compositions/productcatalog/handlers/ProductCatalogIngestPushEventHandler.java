package com.backbase.stream.compositions.productcatalog.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductCatalogIngestPushEvent;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ProductCatalogIngestPushEventHandler
    implements EventHandler<ProductCatalogIngestPushEvent> {

  private final ProductCatalogIngestionService productCatalogIngestionService;
  private final ProductCatalogMapper mapper;

  @Override
  public void handle(EnvelopedEvent<ProductCatalogIngestPushEvent> envelopedEvent) {
    productCatalogIngestionService.ingestPush(buildRequest(envelopedEvent));
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param envelopedEvent EnvelopedEvent<LegalEntityIngestPushEvent>
   * @return LegalEntityIngestPullRequest
   */
  private Mono<ProductCatalogIngestPushRequest> buildRequest(
      EnvelopedEvent<ProductCatalogIngestPushEvent> envelopedEvent) {
    return Mono.just(
        ProductCatalogIngestPushRequest.builder()
            .productCatalog(mapper.mapEventToStream(envelopedEvent.getEvent().getProductCatalog()))
            .build());
  }
}
