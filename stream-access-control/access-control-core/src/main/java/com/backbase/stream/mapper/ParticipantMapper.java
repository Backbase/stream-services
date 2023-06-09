package com.backbase.stream.mapper;

import com.backbase.dbs.accesscontrol.api.service.v2.model.PresentationParticipantBatchUpdate;
import com.backbase.dbs.accesscontrol.api.service.v2.model.PresentationParticipantPutBody;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ParticipantMapper {

  PresentationParticipantBatchUpdate toPresentation(ServiceAgreement serviceAgreement);

  @Mapping(source = "externalId", target = "externalParticipantId")
  PresentationParticipantPutBody toPresentation(LegalEntityParticipant participant);

  @AfterMapping
  default void afterMapping(
      ServiceAgreement serviceAgreement,
      @MappingTarget PresentationParticipantBatchUpdate participants) {
    if (participants.getParticipants() != null) {
      participants
          .getParticipants()
          .forEach(
              participant ->
                  participant.setExternalServiceAgreementId(serviceAgreement.getExternalId()));
    }
  }
}
