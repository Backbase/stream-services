package com.backbase.stream.mapper;

import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.UpdateParticipantItem;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ParticipantMapper {

    @Mapping(source = "externalId", target = "externalLegalEntityId")
    @Mapping(source = "java(externalServiceAgreementId)", target = "externalServiceAgreementId")
    UpdateParticipantItem toPresentation(LegalEntityParticipant participant, String externalServiceAgreementId);

}
