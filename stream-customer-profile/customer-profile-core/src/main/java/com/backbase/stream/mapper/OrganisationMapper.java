package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.OrganisationUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.OrganisationUpsertDtoLegalStructure;
import com.backbase.stream.legalentity.model.Organisation;
import com.backbase.stream.legalentity.model.OrganisationLegalStructure;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {IdentificationMapper.class}
)
public interface OrganisationMapper {

    OrganisationMapper INSTANCE = Mappers.getMapper(OrganisationMapper.class);

    OrganisationUpsertDto organisationToOrganisationUpsertDto(Organisation source);

    OrganisationUpsertDtoLegalStructure legalStructureToDto(OrganisationLegalStructure source);

}