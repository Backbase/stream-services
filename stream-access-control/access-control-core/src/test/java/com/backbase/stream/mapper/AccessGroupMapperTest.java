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
        final String serviceAgreementId = "someServiceAgreementId";
        final String externalId = "someExternalId";
        final String serviceAgreementName = "someServiceAgreementName";
        final String description = "someDescription";
        final String purpose = "somePurpose";
        final Boolean serviceAgreementMaster = true;
        final CustomerCategory customerCategory = CustomerCategory.RETAIL;

        UserContextItem input = new UserContextItem()
            .serviceAgreementId(serviceAgreementId)
            .externalId(externalId)
            .serviceAgreementName(serviceAgreementName)
            .description(description)
            .purpose(purpose)
            .serviceAgreementMaster(serviceAgreementMaster)
            .customerCategory(customerCategory);

        ServiceAgreement actual = subject.toStream(input);

        ServiceAgreement expected = new ServiceAgreement()
            .internalId(serviceAgreementId)
            .externalId(externalId)
            .name(serviceAgreementName)
            .description(description)
            .purpose(purpose)
            .isMaster(serviceAgreementMaster)
            .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL);

        assertEquals(expected, actual);
    }

    @Test
    void toStreamMapsListOfUserContextItemsToListOfServiceAgreements() {
        final String serviceAgreementId1 = "someServiceAgreementId1";
        final String externalId1 = "someExternalId1";
        final String serviceAgreementName1 = "someServiceAgreementName1";
        final String description1 = "someDescription1";
        final String purpose1 = "somePurpose1";
        final Boolean serviceAgreementMaster1 = true;
        final CustomerCategory customerCategory1 = CustomerCategory.RETAIL;

        final String serviceAgreementId2 = "someServiceAgreementId2";
        final String externalId2 = "someExternalId2";
        final String serviceAgreementName2 = "someServiceAgreementName2";
        final String description2 = "someDescription2";
        final String purpose2 = "somePurpose2";
        final Boolean serviceAgreementMaster2 = false;
        final CustomerCategory customerCategory2 = CustomerCategory.RETAIL;

        UserContextItem input1 = new UserContextItem()
            .serviceAgreementId(serviceAgreementId1)
            .externalId(externalId1)
            .serviceAgreementName(serviceAgreementName1)
            .description(description1)
            .purpose(purpose1)
            .serviceAgreementMaster(serviceAgreementMaster1)
            .customerCategory(customerCategory1);

        UserContextItem input2 = new UserContextItem()
            .serviceAgreementId(serviceAgreementId2)
            .externalId(externalId2)
            .serviceAgreementName(serviceAgreementName2)
            .description(description2)
            .purpose(purpose2)
            .serviceAgreementMaster(serviceAgreementMaster2)
            .customerCategory(customerCategory2);

        List<UserContextItem> inputList = Arrays.asList(input1, input2);

        List<ServiceAgreement> actualList = subject.toStream(inputList);

        ServiceAgreement expected1 = new ServiceAgreement()
            .internalId(serviceAgreementId1)
            .externalId(externalId1)
            .name(serviceAgreementName1)
            .description(description1)
            .purpose(purpose1)
            .isMaster(serviceAgreementMaster1)
            .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL);

        ServiceAgreement expected2 = new ServiceAgreement()
            .internalId(serviceAgreementId2)
            .externalId(externalId2)
            .name(serviceAgreementName2)
            .description(description2)
            .purpose(purpose2)
            .isMaster(serviceAgreementMaster2)
            .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL);

        List<ServiceAgreement> expectedList = Arrays.asList(expected1, expected2);

        assertEquals(expectedList, actualList);
    }

}
