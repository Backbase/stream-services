package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Action;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.UpdateParticipantItem;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityParticipant.ActionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipantMapperTest {

    private ParticipantMapper subject = new ParticipantMapperImpl();

    @Test
    void mapToPresentationBatchPut() {
        String saExternalId = "someSaExternalId";

        LegalEntityParticipant participant = new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
            .sharingUsers(true).action(ActionEnum.ADD);

        UpdateParticipantItem actual = subject.toPresentation(participant, saExternalId);

        UpdateParticipantItem expected = new UpdateParticipantItem()
            .sharingAccounts(true).sharingUsers(true)
            .action(Action.ADD)
            .externalLegalEntityId("p1")
            .externalServiceAgreementId(saExternalId);
        assertEquals(expected, actual);
    }
}
