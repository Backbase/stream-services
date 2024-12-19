package com.backbase.stream.mapper;

import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ServiceAgreementV2ToV1Mapper {

  ServiceAgreementV2ToV1Mapper INSTANCE = Mappers.getMapper(ServiceAgreementV2ToV1Mapper.class);

  @Mapping(target = "internalId", source = "internalId")
  @Mapping(target = "externalId", source = "externalId")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "description", source = "description")
  @Mapping(target = "purpose", source = "purpose")
  @Mapping(target = "validFromDate", source = "validFromDate")
  @Mapping(target = "validFromTime", source = "validFromTime")
  @Mapping(target = "validUntilDate", source = "validUntilDate")
  @Mapping(target = "validUntilTime", source = "validUntilTime")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "isMaster", source = "isMaster")
  @Mapping(target = "regularUserAps", source = "regularUserAps")
  @Mapping(target = "adminUserAps", source = "adminUserAps")
  @Mapping(target = "jobRoles", source = "jobRoles")
  @Mapping(target = "creatorLegalEntity", source = "creatorLegalEntity")
  @Mapping(target = "limit", source = "limit")
  @Mapping(target = "contacts", source = "contacts")
  @Mapping(target = "additions", source = "additions")
  @Mapping(target = "participants", source = "participants")
  ServiceAgreement map(ServiceAgreementV2 serviceAgreementV2);

  @Mapping(target = "internalId", source = "internalId")
  @Mapping(target = "externalId", source = "externalId")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "description", source = "description")
  @Mapping(target = "validFromDate", source = "validFromDate")
  @Mapping(target = "validFromTime", source = "validFromTime")
  @Mapping(target = "validUntilDate", source = "validUntilDate")
  @Mapping(target = "validUntilTime", source = "validUntilTime")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "isMaster", source = "isMaster")
  @Mapping(target = "regularUserAps", source = "regularUserAps")
  @Mapping(target = "adminUserAps", source = "adminUserAps")
  @Mapping(target = "jobRoles", source = "jobRoles")
  @Mapping(target = "creatorLegalEntity", source = "creatorLegalEntity")
  @Mapping(target = "limit", source = "limit")
  @Mapping(target = "contacts", source = "contacts")
  @Mapping(target = "additions", source = "additions")
  @Mapping(target = "participants", source = "participants")
  ServiceAgreementV2 mapV2(ServiceAgreement serviceAgreement);
}
