package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.PartyPartyRelationshipUpsertDto;
import com.backbase.stream.legalentity.model.PartyRelationship;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PartyRelationshipMapper {

    PartyRelationshipMapper INSTANCE = Mappers.getMapper(PartyRelationshipMapper.class);

    PartyPartyRelationshipUpsertDto relationshipToDto(PartyRelationship source);

    List<PartyPartyRelationshipUpsertDto> relationshipListToDtoList(List<PartyRelationship> source);
}