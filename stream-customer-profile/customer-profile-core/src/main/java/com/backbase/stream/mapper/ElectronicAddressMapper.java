package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.ElectronicAddressesUpsertDto;
import com.backbase.stream.legalentity.model.ElectronicAddress;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {AddressMapper.class}
)
public interface ElectronicAddressMapper {

    ElectronicAddressMapper INSTANCE = Mappers.getMapper(ElectronicAddressMapper.class);

    ElectronicAddressesUpsertDto electronicAddressToDto(ElectronicAddress source);
}