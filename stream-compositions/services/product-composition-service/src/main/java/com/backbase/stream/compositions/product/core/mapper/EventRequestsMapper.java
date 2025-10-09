package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.audit.rest.spec.v3.model.AuditMessage;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPullEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPullEvent;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EventRequestsMapper {

    ProductIngestPullRequest map(ProductPullEvent event);

    TransactionsPullEvent map(TransactionPullIngestionRequest request);

    @Mapping(target = "legalEntityInternalId", source = "legalEntityItem.internalId")
    @Mapping(target = "legalEntityExternalId", source = "legalEntityItem.externalId")
    @Mapping(target = "serviceAgreementExternalId", source = "serviceAgreement.externalId")
    @Mapping(target = "serviceAgreementInternalId", source = "serviceAgreement.internalId")
    @Mapping(target = "userExternalId", source = "auditMessage.username")
    @Mapping(target = "userInternalId", source = "auditMessage.userId")
    @Mapping(target = "additions", source = "auditMessage.additions")
    @Mapping(target = "transactionChainEnabled", source = "transactionChainEnabled")
    @Mapping(target = "paymentOrderChainEnabled", source = "paymentOrderChainEnabled")
    @Mapping(target = "source", constant = "Authentication")
    @Mapping(target = "membershipAccounts", ignore = true)
    @Mapping(target = "referenceJobRoleNames", ignore = true)
    ProductIngestPullRequest map(AuditMessage auditMessage, ServiceAgreement serviceAgreement,
                                 LegalEntity legalEntityItem, Boolean transactionChainEnabled,
                                 Boolean paymentOrderChainEnabled);

    default ZonedDateTime map(OffsetDateTime value) {
        if (value != null) {
            return value.toZonedDateTime();
        }
        return null;
    }

}
