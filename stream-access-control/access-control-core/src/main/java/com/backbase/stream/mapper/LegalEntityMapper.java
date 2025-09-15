package com.backbase.stream.mapper;

import com.backbase.accesscontrol.legalentity.api.integration.v3.model.SingleServiceAgreement;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityUpdate;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityWithParent;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LegalEntityMapper {

    @Mapping(source = "legalEntityType", target = "type")
    @Mapping(source = "activateSingleServiceAgreement", target = "createSingleServiceAgreement")
    com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityItem toPresentation(
        LegalEntity legalEntity);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    LegalEntity toStream(
        com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntity legalEntityItemBase);

    @Mapping(source = "type", target = "legalEntityType")
    @Mapping(source = "id", target = "internalId")
    @Mapping(source = "parentId", target = "parentInternalId")
    LegalEntity toStream(LegalEntityWithParent legalEntityItem);

    @Mapping(source = "id", target = "internalId")
    @Mapping(constant = "true", target = "isMaster")
    ServiceAgreement toStream(SingleServiceAgreement getServiceAgreement);

    @Mapping(source = "legalEntityType", target = "type")
    LegalEntityUpdate toLegalEntityPut(LegalEntity legalEntity);

}
