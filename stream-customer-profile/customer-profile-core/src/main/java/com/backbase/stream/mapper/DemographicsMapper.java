package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.DemographicsUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.DemographicsUpsertDtoEducation;
import com.backbase.customerprofile.api.integration.v1.model.DemographicsUpsertDtoOccupation;
import com.backbase.stream.legalentity.model.PersonDemographics;
import com.backbase.stream.legalentity.model.PersonDemographicsEducation;
import com.backbase.stream.legalentity.model.PersonDemographicsOccupation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DemographicsMapper {

    DemographicsMapper INSTANCE = Mappers.getMapper(DemographicsMapper.class);

    DemographicsUpsertDto personDemographicsToDemographicsUpsertDto(PersonDemographics source);

    DemographicsUpsertDtoOccupation occupationToDto(PersonDemographicsOccupation source);

    DemographicsUpsertDtoEducation educationToDto(PersonDemographicsEducation source);
}