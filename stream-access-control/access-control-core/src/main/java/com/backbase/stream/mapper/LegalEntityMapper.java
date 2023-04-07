package com.backbase.stream.mapper;

import com.backbase.dbs.accesscontrol.api.service.v2.model.*;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LegalEntityMapper {

  @Mapping(source = "legalEntityType", target = "type")
  LegalEntityCreateItem toPresentation(LegalEntity legalEntity);

  @Mapping(source = "type", target = "legalEntityType")
  @Mapping(source = "id", target = "internalId")
  LegalEntity toStream(LegalEntityItemBase legalEntityItemBase);

  @Mapping(source = "type", target = "legalEntityType")
  @Mapping(source = "id", target = "internalId")
  @Mapping(source = "parentId", target = "parentInternalId")
  LegalEntity toStream(LegalEntityItem legalEntityItem);

  com.backbase.stream.legalentity.model.LegalEntityType map(LegalEntityType legalEntityType);

  LegalEntity toModel(LegalEntityCreateItem legalEntity);

  @Mapping(source = "id", target = "internalId")
  ServiceAgreement toStream(GetServiceAgreement getServiceAgreement);

  @Mapping(source = "additions", target = "legalEntity.additions")
  @Mapping(source = "externalId", target = "legalEntity.externalId")
  @Mapping(source = "name", target = "legalEntity.name")
  @Mapping(source = "legalEntityType", target = "legalEntity.type")
  @Mapping(source = "parentExternalId", target = "legalEntity.parentExternalId")
  @Mapping(
      source = "activateSingleServiceAgreement",
      target = "legalEntity.activateSingleServiceAgreement")
  @Mapping(source = "externalId", target = "externalId")
  LegalEntityPut toLegalEntityPut(LegalEntity legalEntity);
}
