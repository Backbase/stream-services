package com.backbase.stream.mapper;

import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(source = "id", target = "internalId")
    com.backbase.stream.legalentity.model.User toStream(GetUser userItem);

    @Mapping(source = "legalEntityId", target = "legalEntityExternalId")
    User toPresentation(com.backbase.stream.legalentity.model.User user);
}
