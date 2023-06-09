package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.dbs.accesscontrol.api.service.v2.model.CreateStatus;
import com.backbase.dbs.accesscontrol.api.service.v2.model.ParticipantIngest;
import com.backbase.dbs.accesscontrol.api.service.v2.model.PresentationUserApsIdentifiers;
import com.backbase.dbs.accesscontrol.api.service.v2.model.ServiceAgreementItem;
import com.backbase.dbs.accesscontrol.api.service.v2.model.ServiceAgreementPut;
import com.backbase.dbs.accesscontrol.api.service.v2.model.ServicesAgreementIngest;
import com.backbase.dbs.accesscontrol.api.service.v2.model.Status;
import com.backbase.stream.legalentity.model.ApsIdentifiers;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.time.LocalDate;
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

        ServiceAgreement input =
            new ServiceAgreement()
                .externalId(externalId)
                .isMaster(isMaster)
                .description(description)
                .status(LegalEntityStatus.ENABLED)
                .addParticipantsItem(
                    new LegalEntityParticipant().externalId("someLeExId").addUsersItem(userExId))
                .name(name)
                .validFromDate(LocalDate.parse(validFromDate))
                .validFromTime(validFromTime)
                .validUntilDate(LocalDate.parse(validUntilDate))
                .validUntilTime(validUntilTime)
                .regularUserAps(new ApsIdentifiers().addNameIdentifiersItem(userExId));

        ServicesAgreementIngest actual = subject.toPresentation(input);

        ServicesAgreementIngest expected =
            new ServicesAgreementIngest()
                .externalId(externalId)
                .isMaster(isMaster)
                .description(description)
                .status(CreateStatus.ENABLED)
                .addParticipantsToIngestItem(
                    new ParticipantIngest().externalId("someLeExId").addUsersItem(userExId))
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

        ServiceAgreementItem input =
            new ServiceAgreementItem()
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

        ServiceAgreement expected =
            new ServiceAgreement()
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

        ServiceAgreement input =
            new ServiceAgreement()
                .externalId(externalId)
                .description(description)
                .name(name)
                .validFromDate(LocalDate.parse(validFromDate))
                .validFromTime(validFromTime)
                .validUntilDate(LocalDate.parse(validUntilDate))
                .validUntilTime(validUntilTime)
                .status(status);

        ServiceAgreementPut actual = subject.toPresentationPut(input);

        ServiceAgreementPut expected =
            new ServiceAgreementPut()
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
}
