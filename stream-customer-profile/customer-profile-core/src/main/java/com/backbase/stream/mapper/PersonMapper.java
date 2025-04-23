package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.PersonNameUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PersonUpsertDto;
import com.backbase.stream.legalentity.model.Person;
import com.backbase.stream.legalentity.model.PersonName;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;


@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {IdentificationMapper.class, DemographicsMapper.class}
)
public interface PersonMapper {

    PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);

    PersonUpsertDto personToPersonUpsertDto(Person source);

    PersonNameUpsertDto personNameToPersonNameUpsertDto(PersonName source);
}