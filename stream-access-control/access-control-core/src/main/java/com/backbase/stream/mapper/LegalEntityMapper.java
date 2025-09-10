package com.backbase.stream.mapper;

import com.backbase.accesscontrol.legalentity.api.integration.v3.model.SingleServiceAgreement;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityUpdate;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityWithParent;
import com.backbase.dbs.accesscontrol.api.service.v3.model.GetServiceAgreement;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityCreateItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItemBase;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityPut;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LegalEntityMapper {


    @Mapping(source = "legalEntityType", target = "type")
    @Mapping(source = "createSingleServiceAgreement", target = "activateSingleServiceAgreement")
    com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityItem toPresentation(
        LegalEntity legalEntity);

    @Mapping(source = "legalEntityType", target = "type")
    LegalEntityCreateItem toPresentation(LegalEntityV2 legalEntity);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    LegalEntity toStream(LegalEntityItemBase legalEntityItemBase);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    LegalEntity toStream(
        com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntity legalEntityItemBase);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    LegalEntityV2 toStreamV2(LegalEntityItemBase legalEntityItemBase);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    @Mapping(source = "parentId", target = "parentInternalId")
    LegalEntity toStream(LegalEntityWithParent legalEntityItem);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    @Mapping(source = "parentId", target = "parentInternalId")
    LegalEntityV2 toStreamV2(LegalEntityItem legalEntityItem);

    com.backbase.stream.legalentity.model.LegalEntityType map(LegalEntityType legalEntityType);

    @Mapping(source = "type", target = "legalEntityType")
    LegalEntity toModel(LegalEntityCreateItem legalEntity);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(SingleServiceAgreement getServiceAgreement);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreementV2 toStreamV2(GetServiceAgreement getServiceAgreement);

    @Mapping(source = "legalEntityType", target = "type")
    LegalEntityUpdate toLegalEntityPut(LegalEntity legalEntity);

    @Mapping(source = "additions", target = "newValues.additions")
    @Mapping(source = "externalId", target = "newValues.externalId")
    @Mapping(source = "name", target = "newValues.name")
    @Mapping(source = "legalEntityType", target = "newValues.type")
    @Mapping(source = "customerCategory", target = "newValues.customerCategory")
    @Mapping(source = "parentExternalId", target = "newValues.parentExternalId")
    @Mapping(source = "activateSingleServiceAgreement", target = "newValues.activateSingleServiceAgreement")
    @Mapping(source = "externalId", target = "currentExternalId")
    LegalEntityPut toLegalEntityPut(LegalEntityV2 legalEntity);
}
