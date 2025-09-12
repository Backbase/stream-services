package com.backbase.stream.compositions.product.handlers;

import static java.util.Objects.nonNull;

import com.backbase.audit.persistence.event.spec.v1.AuditMessagesCreatedEvent;
import com.backbase.audit.rest.spec.v3.model.AuditMessage;
import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItemQuery;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ProductFailedEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.EventRequestsMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.AccessControlService;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(ProductConfigurationProperties.class)
@ConditionalOnProperty(prefix = "backbase.stream.compositions.product", name = "login-event.enabled",
        havingValue = "true")
public class ProductPullLoginEventHandler implements EventHandler<AuditMessagesCreatedEvent> {

    private final ProductConfigurationProperties properties;
    private final AccessControlService accessControlService;
    private final ProductIngestionService productIngestionService;
    private final EventRequestsMapper eventRequestsMapper;
    private final EventBus eventBus;
    private final ProductGroupMapper productGroupMapper;

    @Override
    public void handle(EnvelopedEvent<AuditMessagesCreatedEvent> envelopedEvent) {
        ProductConfigurationProperties.LoginEvent loginEventConfig = properties.getLoginEvent();
        envelopedEvent.getEvent().getAuditMessages().forEach(auditMessage -> {
            if (auditMessage.getEventMetaData() != null
                    && nonNull(auditMessage.getEventMetaData().get("Realm"))
                    && loginEventConfig.getRealms().contains(auditMessage.getEventMetaData().get("Realm"))
                    && auditMessage.getEventCategory().equalsIgnoreCase(loginEventConfig.getEventCategory())
                    && auditMessage.getObjectType().equalsIgnoreCase(loginEventConfig.getObjectType())
                    && auditMessage.getEventAction().equalsIgnoreCase(loginEventConfig.getEventAction())
                    && auditMessage.getStatus().equals(loginEventConfig.getStatus())
                    && auditMessage.getSessionId() != null) {

                log.debug("Processing audit message: {}", auditMessage);

                getLegalEntityId(auditMessage)
                        .flatMap(legalEntityId -> getServiceAgreement(auditMessage, legalEntityId)
                                .flatMap(serviceAgreement -> accessControlService.getLegalEntityById(legalEntityId)
                                        .flatMap(legalEntityItem -> {
                                            ProductIngestPullRequest request = eventRequestsMapper
                                                    .map(auditMessage, serviceAgreement,legalEntityItem,
                                                            properties.isTransactionChainEnabled(),
                                                            properties.isPaymentOrderChainEnabled());
                                            return productIngestionService.ingestPull(request);
                                        })))
                        .doOnError(this::handleError)
                        .doOnSuccess(this::handleSuccess)
                        .block();

            } else {
                log.debug("Ignoring audit message: {}", auditMessage);
            }
        });
    }

    /**
     * Get service agreement from id or master service agreement if id is not present
     *
     * @param auditMessage audit message
     * @param legalEntityId legal entity ID
     * @return service agreement
     */
    private Mono<ServiceAgreement> getServiceAgreement(AuditMessage auditMessage, String legalEntityId) {
        return StringUtils.hasText(auditMessage.getServiceAgreementId())
                ? accessControlService.getServiceAgreementById(auditMessage.getServiceAgreementId())
                : accessControlService.getMasterServiceAgreementByInternalLegalEntityId(legalEntityId);
    }

    /**
     * Get legal entity id from external user if not present in audit message
     *
     * @param auditMessage audit message
     * @return legal entity ID
     */
    private Mono<String> getLegalEntityId(AuditMessage auditMessage) {
        return StringUtils.hasText(auditMessage.getLegalEntityId())
                ? Mono.just(auditMessage.getLegalEntityId())
                : accessControlService.getUserByExternalId(auditMessage.getUsername(), true)
                .flatMap(getUser -> Mono.just(getUser.getLegalEntityId()));
    }


    /**
     * Handles response from ingestion service.
     *
     * @param response ProductIngestResponse
     */
    private void handleSuccess(ProductIngestResponse response) {
        if (Boolean.FALSE.equals(properties.getEvents().getEnableCompleted())) {
            return;
        }
        ProductCompletedEvent event = new ProductCompletedEvent()
                .withProductGroups(
                        response.getProductGroups().stream()
                                .map(productGroupMapper::mapStreamToEvent)
                                .toList());

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
        log.error("Error ingesting legal entity using the login event: {}", ex.getMessage());

        if (Boolean.TRUE.equals(properties.getEvents().getEnableFailed())) {
            ProductFailedEvent event = new ProductFailedEvent()
                    .withMessage(ex.getMessage());

            EnvelopedEvent<ProductFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
    }
}
