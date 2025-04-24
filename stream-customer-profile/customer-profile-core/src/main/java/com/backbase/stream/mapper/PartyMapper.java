package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.PartyUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PartyMapper {

    PartyMapper INSTANCE = Mappers.getMapper(PartyMapper.class);

    @Mapping(target = "additions", source = "customFields")
    PartyUpsertDto partyToPartyUpsertDto(Party party);

}