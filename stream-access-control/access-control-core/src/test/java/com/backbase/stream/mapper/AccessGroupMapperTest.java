package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.dbs.accesscontrol.api.service.v3.model.CreateStatus;
import com.backbase.dbs.accesscontrol.api.service.v3.model.CustomerCategory;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ParticipantIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationUserApsIdentifiers;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementPut;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServicesAgreementIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.Status;
import com.backbase.dbs.accesscontrol.api.service.v3.model.UserContextItem;
import com.backbase.stream.legalentity.model.ApsIdentifiers;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
        final String userExId = "someUserExternalId";

        ServiceAgreement input = new ServiceAgreement()
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .status(LegalEntityStatus.ENABLED)
            .addParticipantsItem(new LegalEntityParticipant().externalId("someLeExId").addUsersItem(userExId))
            .name(name)
            .validFromDate(LocalDate.parse(validFromDate))
            .validFromTime(validFromTime)
            .validUntilDate(LocalDate.parse(validUntilDate))
            .validUntilTime(validUntilTime)
            .regularUserAps(new ApsIdentifiers().addNameIdentifiersItem(userExId));


        ServicesAgreementIngest actual = subject.toPresentation(input);


        ServicesAgreementIngest expected = new ServicesAgreementIngest()
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .status(CreateStatus.ENABLED)
            .addParticipantsToIngestItem(new ParticipantIngest().externalId("someLeExId").addUsersItem(userExId))
            .name(name)
            .validFromDate(validFromDate)
            .validFromTime(validFromTime)
            .validUntilDate(validUntilDate)
            .validUntilTime(validUntilTime)
            .regularUserAps(new PresentationUserApsIdentifiers().addNameIdentifiersItem(userExId));
        assertEquals(expected, actual);
    }


    @Test
    void toStreamMapsServiceAgreementItemToServiceAgreement() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final Boolean isMaster = true;
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

        ServiceAgreementItem input = new ServiceAgreementItem()
            .id(internalId)
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .name(name)
            .validFromDate(validFromDate)
            .validFromTime(validFromTime)
            .validUntilDate(validUntilDate)
            .validUntilTime(validUntilTime);


        ServiceAgreement actual = subject.toStream(input);


        ServiceAgreement expected = new ServiceAgreement()
            .internalId(internalId)
            .externalId(externalId)
            .isMaster(isMaster)
            .description(description)
            .name(name)
            .validFromDate(LocalDate.parse(validFromDate))
            .validFromTime(validFromTime)
            .validUntilDate(LocalDate.parse(validUntilDate))
            .validUntilTime(validUntilTime);

        assertEquals(expected, actual);
    }

    @Test
    void toPresentationPutMapsServiceAgreementToServiceAgreementPut() {
        final String externalId = "someExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";
        final LegalEntityStatus status = LegalEntityStatus.ENABLED;

        ServiceAgreement input = new ServiceAgreement()
            .externalId(externalId)
            .description(description)
            .name(name)
            .validFromDate(LocalDate.parse(validFromDate))
            .validFromTime(validFromTime)
            .validUntilDate(LocalDate.parse(validUntilDate))
            .validUntilTime(validUntilTime)
            .status(status);


        ServiceAgreementPut actual = subject.toPresentationPut(input);


        ServiceAgreementPut expected = new ServiceAgreementPut()
            .externalId(externalId)
            .description(description)
            .name(name)
            .validFromDate(validFromDate)
            .validFromTime(validFromTime)
            .validUntilDate(validUntilDate)
            .validUntilTime(validUntilTime)
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

    @Test
    void toStreamMapsListOfUserContextItemsToListOfServiceAgreements() {
        var userContextItem1 = createUserContextItem("someServiceAgreementId1", "someExternalId1",
            "someServiceAgreementName1",
            "someDescription1", "somePurpose1", true);
        var userContextItem2 = createUserContextItem("someServiceAgreementId2", "someExternalId2",
            "someServiceAgreementName2",
            "someDescription2", "somePurpose2", false);

        var userContexts = List.of(userContextItem1, userContextItem2);

        var actualServiceAgreements = subject.toStream(userContexts);

        var expectedServiceAgreements = Arrays.asList(
            createServiceAgreementFromUserContextItem(userContextItem1),
            createServiceAgreementFromUserContextItem(userContextItem2)
        );

        assertEquals(expectedServiceAgreements, actualServiceAgreements);
    }

    private UserContextItem createUserContextItem(String serviceAgreementId, String externalId,
        String serviceAgreementName,
        String description, String purpose, Boolean serviceAgreementMaster) {
        return new UserContextItem()
            .serviceAgreementId(serviceAgreementId)
            .externalId(externalId)
            .serviceAgreementName(serviceAgreementName)
            .description(description)
            .purpose(purpose)
            .serviceAgreementMaster(serviceAgreementMaster)
            .customerCategory(CustomerCategory.RETAIL);
    }

    private ServiceAgreement createServiceAgreementFromUserContextItem(UserContextItem item) {
        return new ServiceAgreement()
            .internalId(item.getServiceAgreementId())
            .externalId(item.getExternalId())
            .name(item.getServiceAgreementName())
            .description(item.getDescription())
            .purpose(item.getPurpose())
            .isMaster(item.getServiceAgreementMaster())
            .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL);
    }

}
