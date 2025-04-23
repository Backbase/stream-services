package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.IdentificationUpsertDto;
import com.backbase.stream.legalentity.model.Identification;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface IdentificationMapper {

    IdentificationMapper INSTANCE = Mappers.getMapper(IdentificationMapper.class);

    IdentificationUpsertDto identificationToIdentificationUpsertDto(Identification source);

    List<IdentificationUpsertDto> identificationListToIdentificationUpsertDtoList(List<Identification> source);
}