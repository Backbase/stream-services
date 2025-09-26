package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Admin;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ParticipantCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementDetails;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.User;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Status;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.ContextServiceAgreement;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.CustomerCategory;
import com.backbase.stream.legalentity.model.ApsIdentifiers;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessGroupMapperTest {

    private AccessGroupMapper subject = new AccessGroupMapperImpl();

    private static final OffsetDateTime VALID_FROM = OffsetDateTime.of(LocalDateTime.now().minusYears(1L),
        ZoneOffset.UTC);
    private static final OffsetDateTime VALID_UNTIL = OffsetDateTime.of(LocalDateTime.now().plusMonths(10L),
        ZoneOffset.UTC);

    @Test
    void toPresentationServiceAgreementIngest() {
        final String externalId = "someExternalId";
        final Boolean isMaster = true;
        final String description = "someDescription";
        final String name = "someName";

        ServiceAgreement input = new ServiceAgreement()
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .status(null)
            .addParticipantsItem(new LegalEntityParticipant().externalId("someId").addUsersItem("userId")
                .addAdminsItem("adminId"))
            .name(name)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .regularUserAps(new ApsIdentifiers().addNameIdentifiersItem("regularUserAps-1"))
            .adminUserAps(new ApsIdentifiers().addNameIdentifiersItem("adminUserAps-1"));

        ServiceAgreementCreateRequest actual = subject.map(input);

        ServiceAgreementCreateRequest expected = new ServiceAgreementCreateRequest()
            .externalId(externalId)
            .isSingle(isMaster)
            .description(description)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .status(com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Status.ENABLED)
            .participants(List.of(new ParticipantCreateRequest().externalId("someId")
                .addUsersItem(new User().externalUserId("userId"))
                .addAdminsItem(new Admin().externalUserId("adminId"))))
            .name(name)
            .regularUserApsNames(List.of("regularUserAps-1"))
            .adminUserApsNames(List.of("adminUserAps-1"));
        assertEquals(expected, actual);
    }


    @Test
    void toStreamMapsServiceAgreementItemToServiceAgreement() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final Boolean isMaster = true;
        final String description = "someDescription";
        final String name = "someName";

        ServiceAgreementDetails input = new ServiceAgreementDetails()
            .id(internalId)
            .externalId(externalId)
            .isSingle(isMaster)
            .description(description)
            .name(name)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .purpose("somePurpose")
            .status(com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Status.ENABLED)
            .creatorLegalEntityId("someCreatorId");

        ServiceAgreement actual = subject.toStream(input);

        ServiceAgreement expected = new ServiceAgreement()
            .internalId(internalId)
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .name(name)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .status(LegalEntityStatus.ENABLED)
            .purpose("somePurpose")
            .creatorLegalEntity("someCreatorId");

        assertEquals(expected, actual);
    }

    @Test
    void toPresentationPutMapsServiceAgreementToServiceAgreementPut() {
        final String externalId = "someExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final LegalEntityStatus status = LegalEntityStatus.ENABLED;

        ServiceAgreement input = new ServiceAgreement()
            .externalId(externalId)
            .description(description)
            .name(name)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .purpose("somePurpose")
            .status(status);

        ServiceAgreementUpdateRequest actual = subject.toPresentationPut(input);

        ServiceAgreementUpdateRequest expected = new ServiceAgreementUpdateRequest()
            .externalId(externalId)
            .description(description)
            .name(name)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .purpose("somePurpose")
            .status(Status.ENABLED);

        assertEquals(expected, actual);
    }

    @Test
    void toStreamMapsUserContextItemToServiceAgreement() {
        var userContextItem = new ContextServiceAgreement()
            .id("someServiceAgreementId")
            .externalId("someExternalId")
            .name("someServiceAgreementName")
            .description("someDescription")
            .purpose("somePurpose")
            .isSingle(true)
            .customerCategory(CustomerCategory.RETAIL);

        var actualServiceAgreement = subject.toStream(userContextItem);

        var expectedServiceAgreement = new ServiceAgreement()
            .internalId(userContextItem.getId())
            .externalId(userContextItem.getExternalId())
            .name(userContextItem.getName())
            .description(userContextItem.getDescription())
            .purpose(userContextItem.getPurpose())
            .isMaster(userContextItem.getIsSingle())
            .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL);

        assertEquals(expectedServiceAgreement, actualServiceAgreement);
    }

}
