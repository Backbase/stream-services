package com.backbase.stream.mapper;

import static org.mapstruct.NullValueMappingStrategy.RETURN_NULL;

import com.backbase.dbs.contact.api.service.v2.model.ExternalContact;
import java.util.List;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExternalContactMapper {

    ExternalContactMapper INSTANCE = Mappers.getMapper(ExternalContactMapper.class);

    @IterableMapping(nullValueMappingStrategy = RETURN_NULL)
    List<ExternalContact> toMapList(
        List<com.backbase.stream.legalentity.model.ExternalContact> externalContacts);

    ExternalContact map(com.backbase.stream.legalentity.model.ExternalContact externalContact);
}
