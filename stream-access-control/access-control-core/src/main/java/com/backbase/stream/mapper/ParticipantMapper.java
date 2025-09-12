package com.backbase.stream.mapper;

import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.UpdateParticipantItem;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ParticipantMapper {

    @Mapping(source = "participant.externalId", target = "externalLegalEntityId")
    @Mapping(source = "externalServiceAgreementId", target = "externalServiceAgreementId")
    @Mapping(source = "participant.action", target = "action")
    @Mapping(source = "participant.sharingUsers", target = "sharingUsers")
    @Mapping(source = "participant.sharingAccounts", target = "sharingAccounts")
    UpdateParticipantItem toPresentation(LegalEntityParticipant participant, String externalServiceAgreementId);

}
