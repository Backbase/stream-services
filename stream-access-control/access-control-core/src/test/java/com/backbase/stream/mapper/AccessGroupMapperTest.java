package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementDetails;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Admin;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ParticipantWithAdminsAndUsers;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Status;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.ContextServiceAgreement;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.CustomerCategory;
import com.backbase.stream.legalentity.model.ApsIdentifiers;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessGroupMapperTest {

    private AccessGroupMapper subject = new AccessGroupMapperImpl();

    @Test
    void toPresentationServiceAgreementIngest() {
        final String externalId = "someExternalId";
        final Boolean isMaster = true;
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

        ServiceAgreement input = new ServiceAgreement()
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .status(LegalEntityStatus.ENABLED)
            .addParticipantsItem(new LegalEntityParticipant().internalId("someId").addUsersItem("userId")
                .addAdminsItem("adminId"))
            .name(name)
            .validFromDate(LocalDate.parse(validFromDate))
            .validFromTime(validFromTime)
            .validUntilDate(LocalDate.parse(validUntilDate))
            .validUntilTime(validUntilTime)
            .regularUserAps(new ApsIdentifiers().addIdIdentifiersItem(BigDecimal.ONE));

        ServiceAgreementCreateRequest actual = subject.map(input);

        ServiceAgreementCreateRequest expected = new ServiceAgreementCreateRequest()
            .externalId(externalId)
            .isSingle(isMaster)
            .description(description)
            .status(Status.ENABLED)
            .participants(List.of(new ParticipantWithAdminsAndUsers().legalEntityId("someId")
                .addUsersItem(new User().userId("userId"))
                .addAdminsItem(new Admin().userId("adminId"))))
            .name(name)
            .regularUserApsIds(Set.of(1L));
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
            .name(name);

        ServiceAgreement actual = subject.toStream(input);

        ServiceAgreement expected = new ServiceAgreement()
            .internalId(internalId)
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .name(name);

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
            .status(status);

        ServiceAgreementUpdateRequest actual = subject.toPresentationPut(input);

        ServiceAgreementUpdateRequest expected = new ServiceAgreementUpdateRequest()
            .externalId(externalId)
            .description(description)
            .name(name)
            .status(Status.ENABLED);

        assertEquals(expected, actual);
    }

    @Test
    void toStreamMapsUserContextItemToServiceAgreement() {
        var userContextItem = createUserContextItem("someServiceAgreementId", "someExternalId",
            "someServiceAgreementName",
            "someDescription", "somePurpose", true);

        var actualServiceAgreement = subject.toStream(userContextItem);

        var expectedServiceAgreement = createServiceAgreementFromUserContextItem(userContextItem);

        assertEquals(expectedServiceAgreement, actualServiceAgreement);
    }

    private ContextServiceAgreement createUserContextItem(String serviceAgreementId, String externalId,
        String serviceAgreementName, String description, String purpose, Boolean serviceAgreementMaster) {
        return new ContextServiceAgreement()
            .id(serviceAgreementId)
            .externalId(externalId)
            .name(serviceAgreementName)
            .description(description)
            .purpose(purpose)
            .isSingle(serviceAgreementMaster)
            .customerCategory(CustomerCategory.RETAIL);
    }

    private ServiceAgreement createServiceAgreementFromUserContextItem(ContextServiceAgreement item) {
        return new ServiceAgreement()
            .internalId(item.getId())
            .externalId(item.getExternalId())
            .name(item.getName())
            .description(item.getDescription())
            .purpose(item.getPurpose())
            .isMaster(item.getIsSingle())
            .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL);
    }

}
