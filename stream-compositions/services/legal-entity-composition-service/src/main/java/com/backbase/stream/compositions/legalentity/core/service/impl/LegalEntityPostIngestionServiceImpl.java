package com.backbase.stream.compositions.legalentity.core.service.impl;

import static java.util.Objects.nonNull;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityFailedEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityPostIngestionService;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.backbase.stream.compositions.product.client.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.client.model.ProductPullIngestionRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class LegalEntityPostIngestionServiceImpl implements LegalEntityPostIngestionService {

  private final EventBus eventBus;

  private final LegalEntityConfigurationProperties config;

  private final ProductCompositionApi productCompositionApi;

  private final LegalEntityMapper mapper;

  @Override
  public Mono<LegalEntityResponse> handleSuccess(LegalEntityResponse res) {
    return Mono.just(res)
        .doOnNext(
            r ->
                log.info(
                    "Legal entities ingestion completed successfully. {}",
                    res.getLegalEntity().getInternalId()))
        .flatMap(this::processChains)
        .doOnNext(this::processSuccessEvent)
        .doOnNext(r -> log.debug("Ingested legal entity: {}", res.getLegalEntity()));
  }

  private Mono<LegalEntityResponse> processChains(LegalEntityResponse res) {
    Mono<LegalEntityResponse> productChainMono;
    boolean isProductChainEnabled =
        res.getProductChainEnabledFromRequest() == null
            ? config.isProductChainEnabled()
            : res.getProductChainEnabledFromRequest();

    if (!isProductChainEnabled) {
      log.debug("Product Chain is disabled");
      productChainMono = Mono.just(res);
    } else if (config.isProductChainAsync()) {
      productChainMono = ingestProductsAsync(res);
    } else {
      productChainMono = ingestProducts(res);
    }

    return productChainMono;
  }

  private Mono<LegalEntityResponse> ingestProducts(LegalEntityResponse res) {
    return buildProductPullRequest(res)
        .flatMap(productCompositionApi::pullIngestProduct)
        .onErrorResume(this::handleProductError)
        .doOnNext(
            response ->
                log.debug("Response from Product Composition: {}", response.getProductGroups()))
        .last()
        .map(p -> res);
  }

  private Mono<LegalEntityResponse> ingestProductsAsync(LegalEntityResponse res) {
    return buildProductPullRequest(res)
        .collectList()
        .doOnNext(
            requests ->
                requests.forEach(
                    r ->
                        productCompositionApi
                            .pullIngestProduct(r)
                            .subscribe(t -> log.debug("Async product" + " ingestion" + " called"))))
        .map(p -> res);
  }

  private void processSuccessEvent(LegalEntityResponse res) {
    if (Boolean.TRUE.equals(config.isCompletedEventEnabled())) {
      LegalEntityCompletedEvent event =
          new LegalEntityCompletedEvent()
              .withLegalEntity(mapper.mapStreamToEvent(res.getLegalEntity()));
      EnvelopedEvent<LegalEntityCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
      envelopedEvent.setEvent(event);
      eventBus.emitEvent(envelopedEvent);
    }
  }

  public void handleFailure(Throwable error) {
    log.error("Legal entities ingestion failed. {}", error.getMessage());
    if (Boolean.TRUE.equals(config.isFailedEventEnabled())) {
      LegalEntityFailedEvent event =
          new LegalEntityFailedEvent()
              .withEventId(UUID.randomUUID().toString())
              .withMessage(error.getMessage());
      EnvelopedEvent<LegalEntityFailedEvent> envelopedEvent = new EnvelopedEvent<>();
      envelopedEvent.setEvent(event);
      eventBus.emitEvent(envelopedEvent);
    }
  }

  private Flux<ProductPullIngestionRequest> buildProductPullRequest(LegalEntityResponse res) {
    List<ProductPullIngestionRequest> requests =
        buildProductPullRequest(
            res.getLegalEntity(), res.getMembershipAccounts(), res.getAdditions());
    return Flux.fromIterable(requests);
  }

  private List<ProductPullIngestionRequest> buildProductPullRequest(
      LegalEntity legalEntity, List<String> membershipAccounts, Map<String, String> additions) {
    List<ProductPullIngestionRequest> requests = new ArrayList<>();

    ServiceAgreement serviceAgreement = getServiceAgreementFromLegalEntity(legalEntity);
    User user = getUserFromLegalEntity(legalEntity);
    List<String> referenceJobRoleNames = getReferenceobRoleNamesFromLegalEntity(legalEntity);

    requests.add(
        new ProductPullIngestionRequest()
            .withLegalEntityInternalId(legalEntity.getInternalId())
            .withLegalEntityExternalId(legalEntity.getExternalId())
            .withServiceAgreementExternalId(serviceAgreement.getExternalId())
            .withServiceAgreementInternalId(serviceAgreement.getInternalId())
            .withUserExternalId(user.getExternalId())
            .withUserInternalId(user.getInternalId())
            .withReferenceJobRoleNames(referenceJobRoleNames)
            .withMembershipAccounts(membershipAccounts)
            .withAdditions(additions));

    if (config.getChains().getIncludeSubsidiaries()
        && !CollectionUtils.isEmpty(legalEntity.getSubsidiaries())) {
      log.info(
          "Processing {} subsidiaries for legal entity '{}'",
          legalEntity.getSubsidiaries().size(),
          legalEntity.getExternalId());
      legalEntity
          .getSubsidiaries()
          .forEach(
              subsidiary ->
                  requests.addAll(
                      buildProductPullRequest(subsidiary, membershipAccounts, additions)));
    } else {
      log.debug("No subsidiary chained for legal entity '{}'.", legalEntity.getExternalId());
    }

    return requests;
  }

  private User getUserFromLegalEntity(LegalEntity lg) {
    if (nonNull(lg.getUsers()) && !lg.getUsers().isEmpty()) {
      return lg.getUsers().get(0).getUser();
    } else {
      return new User();
    }
  }

  private List<String> getReferenceobRoleNamesFromLegalEntity(LegalEntity lg) {
    if (nonNull(lg.getUsers())
        && !lg.getUsers().isEmpty()
        && nonNull(lg.getUsers().get(0).getReferenceJobRoleNames())) {
      return lg.getUsers().get(0).getReferenceJobRoleNames();
    } else {
      return new ArrayList<>();
    }
  }

  private ServiceAgreement getServiceAgreementFromLegalEntity(LegalEntity lg) {
    return nonNull(lg.getMasterServiceAgreement())
        ? lg.getMasterServiceAgreement()
        : lg.getCustomServiceAgreement();
  }

  private Mono<ProductIngestionResponse> handleProductError(Throwable t) {
    log.error("Error while calling Product Composition: {}", t.getMessage());
    return Mono.error(new InternalServerErrorException(t.getMessage()));
  }
}
