package com.backbase.stream.mapper;

import com.backbase.dbs.accesscontrol.api.service.v3.model.GetServiceAgreement;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityCreateItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItemBase;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityPut;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType;
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

    @Mapping(source = "additions", target = "newValues.additions")
    @Mapping(source = "externalId", target = "newValues.externalId")
    @Mapping(source = "name", target = "newValues.name")
    @Mapping(source = "legalEntityType", target = "newValues.type")
    @Mapping(source = "customerCategory", target = "newValues.customerCategory")
    @Mapping(source = "parentExternalId", target = "newValues.parentExternalId")
    @Mapping(source = "activateSingleServiceAgreement", target = "newValues.activateSingleServiceAgreement")
    @Mapping(source = "externalId", target = "currentExternalId")
    LegalEntityPut toLegalEntityPut(LegalEntity legalEntity);
}
