package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.dbs.accesscontrol.api.service.v2.model.PresentationAction;
import com.backbase.dbs.accesscontrol.api.service.v2.model.PresentationParticipantBatchUpdate;
import com.backbase.dbs.accesscontrol.api.service.v2.model.PresentationParticipantPutBody;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipantMapperTest {

    private ParticipantMapper subject = new ParticipantMapperImpl();

    @Test
    void mapToPresentationBatchPut() {
        String saExternalId = "someSaExternalId";
        ServiceAgreement serviceAgreement =
            new ServiceAgreement()
                .externalId(saExternalId)
                .addParticipantsItem(
                    new LegalEntityParticipant()
                        .externalId("p1")
                        .sharingAccounts(true)
                        .sharingUsers(true)
                        .action(LegalEntityParticipant.ActionEnum.ADD))
                .addParticipantsItem(
                    new LegalEntityParticipant()
                        .externalId("p2")
                        .sharingAccounts(false)
                        .sharingUsers(false)
                        .action(LegalEntityParticipant.ActionEnum.REMOVE));

        PresentationParticipantBatchUpdate actual = subject.toPresentation(serviceAgreement);

        PresentationParticipantBatchUpdate expected = new PresentationParticipantBatchUpdate();
        expected.addParticipantsItem(
            new PresentationParticipantPutBody()
                .sharingAccounts(true)
                .sharingUsers(true)
                .action(PresentationAction.ADD)
                .externalParticipantId("p1")
                .externalServiceAgreementId(saExternalId));
        expected.addParticipantsItem(
            new PresentationParticipantPutBody()
                .sharingAccounts(false)
                .sharingUsers(false)
                .action(PresentationAction.REMOVE)
                .externalParticipantId("p2")
                .externalServiceAgreementId(saExternalId));
        assertEquals(expected, actual);
    }
}
