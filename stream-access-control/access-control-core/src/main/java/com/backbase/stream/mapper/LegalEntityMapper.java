package com.backbase.stream.mapper;

import com.backbase.dbs.accesscontrol.query.service.model.ServiceAgreementItem;
import com.backbase.dbs.legalentity.presentation.service.model.*;
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
    @Mapping(source = "id",target = "internalId")
    LegalEntity toStream(LegalEntityItem legalEntityItem);

    LegalEntityType map(com.backbase.dbs.legalentity.presentation.service.model.LegalEntityType legalEntityType);


    LegalEntity toModel(LegalEntityCreateItem legalEntity);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(GetServiceAgreement getServiceAgreement);
}
