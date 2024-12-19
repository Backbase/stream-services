package com.backbase.stream.mapper;

import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;

@Mapper(uses = ServiceAgreementV2ToV1Mapper.class)
public interface LegalEntityV2toV1Mapper {

    @Mappings({
        @Mapping(target = "internalId", source = "internalId"),
        @Mapping(target = "externalId", source = "externalId"),
        @Mapping(target = "name", source = "name"),
        @Mapping(target = "activateSingleServiceAgreement", source = "activateSingleServiceAgreement"),
        @Mapping(target = "legalEntityType", source = "legalEntityType"),
        @Mapping(target = "customerCategory", source = "customerCategory"),
        @Mapping(target = "realmName", source = "realmName"),
        @Mapping(target = "parentExternalId", source = "parentExternalId"),
        @Mapping(target = "parentInternalId", source = "parentInternalId"),
        @Mapping(target = "subsidiaries", qualifiedByName = "mapLegalEntities"),
        @Mapping(target = "limit", source = "limit"),
        @Mapping(target = "users", qualifiedByName = "mapUsers"),
        @Mapping(target = "masterServiceAgreement", qualifiedByName = "mapServiceAgreement")
    })
    @Named("mapLegalEntities")
    @IterableMapping(qualifiedByName = "mapLegalEntity")
    List<LegalEntity> mapLegalEntityV2ListToLegalEntityList(List<LegalEntityV2> legalEntitiesV2);

    @Mappings({
        @Mapping(target = "internalId", source = "internalId"),
        @Mapping(target = "externalId", source = "externalId"),
        @Mapping(target = "name", source = "name"),
        @Mapping(target = "activateSingleServiceAgreement", source = "activateSingleServiceAgreement"),
        @Mapping(target = "legalEntityType", source = "legalEntityType"),
        @Mapping(target = "customerCategory", source = "customerCategory"),
        @Mapping(target = "realmName", source = "realmName"),
        @Mapping(target = "parentExternalId", source = "parentExternalId"),
        @Mapping(target = "parentInternalId", source = "parentInternalId"),
        @Mapping(target = "subsidiaries", qualifiedByName = "mapLegalEntitiesV2"),
        @Mapping(target = "limit", source = "limit"),
        @Mapping(target = "users", qualifiedByName = "mapUsers"),
        @Mapping(target = "masterServiceAgreement", qualifiedByName = "mapServiceAgreementV2")
    })
    @Named("mapLegalEntitiesV2")
    @IterableMapping(qualifiedByName = "mapLegalEntityV2")
    List<LegalEntityV2> mapLegalEntityListToLegalEntityV2List(List<LegalEntity> legalEntities);

    @Named("mapLegalEntity")
    LegalEntity mapLegalEntityV2ToLegalEntity(LegalEntityV2 legalEntityV2);


    @Named("mapLegalEntityV2")
    LegalEntityV2 mapLegalEntityToLegalEntityV2(LegalEntity legalEntity);

    @Named("mapUsers")
    @IterableMapping(qualifiedByName = "mapUser")
    List<JobProfileUser> mapUsers(List<User> users);

    @Named("mapUser")
    default JobProfileUser mapUser(User user) {
        return new JobProfileUser().user(user);
    }

    @Named("mapServiceAgreement")
    default ServiceAgreement mapServiceAgreementV2ToServiceAgreement(ServiceAgreementV2 serviceAgreementV2,
        @Context ServiceAgreementV2ToV1Mapper serviceAgreementMapper) {
        return serviceAgreementMapper.map(serviceAgreementV2);
    }

    @Named("mapServiceAgreementV2")
    default ServiceAgreementV2 mapServiceAgreementToServiceAgreementV2(ServiceAgreement serviceAgreement,
        @Context ServiceAgreementV2ToV1Mapper serviceAgreementMapper) {
        return serviceAgreementMapper.mapV2(serviceAgreement);
    }
}

