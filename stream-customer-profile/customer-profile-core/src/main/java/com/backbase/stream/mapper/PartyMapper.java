package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.PartyUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PostalAddressDto;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.legalentity.model.PartyPostalAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PartyMapper {

    @Mapping(target = "additions", source = "customFields")
    PartyUpsertDto partyToPartyUpsertDto(Party party);

    @ValueMapping(source = "BUSINESS", target = "BUSINESS")
    @ValueMapping(source = "CORRESPONDENCE", target = "CORRESPONDENCE")
    @ValueMapping(source = "DELIVERY_TO", target = "DELIVERYTO")
    @ValueMapping(source = "MAIL_TO", target = "MAILTO")
    @ValueMapping(source = "PO_BOX", target = "PO_BOX")
    @ValueMapping(source = "POSTAL", target = "POSTAL")
    @ValueMapping(source = "RESIDENTIAL", target = "RESIDENTIAL")
    @ValueMapping(source = "STATEMENT", target = "STATEMENT")
    PostalAddressDto mapPostalAddressType(PartyPostalAddress partyPostalAddress);

}