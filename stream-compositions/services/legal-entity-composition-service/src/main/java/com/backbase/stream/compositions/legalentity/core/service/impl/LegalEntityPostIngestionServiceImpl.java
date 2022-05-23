package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityFailedEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityPostIngestionService;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.backbase.stream.compositions.product.client.model.ProductPullIngestionRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class LegalEntityPostIngestionServiceImpl implements LegalEntityPostIngestionService {

    private final EventBus eventBus;

    private final LegalEntityConfigurationProperties legalEntityConfigurationProperties;

    private final ProductCompositionApi productCompositionApi;


    @Override
    public void handleSuccess(LegalEntityResponse res) {
        log.info("Legal entities ingestion completed successfully. {}", res);
        if (Boolean.TRUE.equals(legalEntityConfigurationProperties.getChains().getProductComposition().getEnableOnComplete())) {
            log.info("Call product-composition-service for Legal Entity {}", res.getLegalEntity().getInternalId());
            sendProductPullEvent(res);
        }

        if (log.isDebugEnabled()) {
            log.debug("Ingested legal entity: {}", res.getLegalEntity());
        }


    }

    public Mono<LegalEntityResponse> handleFailure(Throwable error) {
        log.error("Legal entities ingestion failed. {}", error.getMessage());
        if (Boolean.TRUE.equals(legalEntityConfigurationProperties.getEvents().getEnableFailed())) {
            LegalEntityFailedEvent event = new LegalEntityFailedEvent().withEventId(UUID.randomUUID().toString())
                    .withMessage(error.getMessage());
            EnvelopedEvent<LegalEntityFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
        return Mono.empty();
    }

    private void sendProductPullEvent(LegalEntityResponse res) {
        LegalEntity legalEntity = res.getLegalEntity();

        if (CollectionUtils.isEmpty(legalEntity.getUsers())) {
            log.error("Legalentity is missing users. Cannot call product-composition");
            return;
        }

        ProductPullIngestionRequest productPullIngestionRequest =
                new ProductPullIngestionRequest().withLegalEntityExternalId(legalEntity.getExternalId())
                        .withServiceAgreementExternalId(legalEntity.getMasterServiceAgreement().getExternalId())
                        .withServiceAgreementInternalId(legalEntity.getMasterServiceAgreement().getInternalId())
                        .withMembershipAccounts(res.getMembershipAccounts());


        productPullIngestionRequest.setUserExternalId(
                legalEntity.getUsers().get(0).getUser().getExternalId());

        productCompositionApi.pullIngestProduct(productPullIngestionRequest)
                .onErrorResume(e -> {
                    log.info(e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
