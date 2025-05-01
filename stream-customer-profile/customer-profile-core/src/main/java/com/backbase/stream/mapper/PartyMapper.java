package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.PartyUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PartyMapper {

    @Mapping(target = "additions", source = "customFields")
    PartyUpsertDto partyToPartyUpsertDto(Party party);

}