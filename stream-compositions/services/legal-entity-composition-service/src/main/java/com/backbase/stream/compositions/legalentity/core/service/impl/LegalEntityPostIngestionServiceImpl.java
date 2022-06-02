package com.backbase.stream.compositions.legalentity.core.service.impl;

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
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.User;
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

    private final LegalEntityMapper legalEntityMapper;


    @Override
    public void handleSuccess(LegalEntityResponse res) {
        log.info("Legal entities ingestion completed successfully. {}", res);
        if (Boolean.TRUE.equals(legalEntityConfigurationProperties.getChains().getProductComposition().getEnableOnComplete())) {
            log.info("Call product-composition-service for Legal Entity {}", res.getLegalEntity().getInternalId());
            sendProductPullEvent(res);
        }

        if (Boolean.TRUE.equals(legalEntityConfigurationProperties.getEvents().getEnableCompleted())) {
            LegalEntityCompletedEvent event = new LegalEntityCompletedEvent()
                    .withLegalEntity(legalEntityMapper.mapStreamToEvent(res.getLegalEntity()));
            EnvelopedEvent<LegalEntityCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
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

        JobProfileUser jpUser = legalEntity.getUsers().get(0);
        User user = jpUser.getUser();

        ProductPullIngestionRequest productPullIngestionRequest =
                new ProductPullIngestionRequest()
                        .withLegalEntityInternalId(legalEntity.getInternalId())
                        .withLegalEntityExternalId(legalEntity.getExternalId())
                        .withServiceAgreementExternalId(legalEntity.getMasterServiceAgreement().getExternalId())
                        .withServiceAgreementInternalId(legalEntity.getMasterServiceAgreement().getInternalId())
                        .withMembershipAccounts(res.getMembershipAccounts())
                        .withUserExternalId(user.getExternalId())
                        .withUserInternalId(user.getInternalId())
                        .withReferenceJobRoleNames(jpUser.getReferenceJobRoleNames());


        if (Boolean.FALSE.equals(legalEntityConfigurationProperties.getChains().getProductComposition().getAsync())) {
            productCompositionApi.pullIngestProduct(productPullIngestionRequest)
                    .doOnSuccess(response -> log.info("Received Response from Product Composition: ID :: {}", response.getProductGgroup().getInternalId()))
                    .onErrorResume(this::handleProductError)
                    .subscribe();
        } else {
            productCompositionApi.pullIngestProductAsync(productPullIngestionRequest)
                    .doOnSuccess(response -> log.info("Product COmposition Call Ended"))
                    .onErrorResume(this::handleAsyncProductError)
                    .subscribe();
        }

    }

    private Mono<Void> handleAsyncProductError(Throwable t) {
        log.error("Error while calling Product Composition asynchronously: {}", t.getMessage());
        return Mono.empty();
    }

    private Mono<ProductIngestionResponse> handleProductError(Throwable t) {
        log.error("Error while calling Product Composition: {}", t.getMessage());
        throw new InternalServerErrorException(t.getMessage());
    }
}
