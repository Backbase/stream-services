package com.backbase.stream.mapper;

import com.backbase.dbs.user.presentation.service.model.CreateUser;
import com.backbase.dbs.user.presentation.service.model.GetUserById;
import com.backbase.stream.legalentity.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {


    @Mapping(source = "id", target = "internalId")
    User toStream(GetUserById userItem);

    @Mapping(source = "legalEntityId", target = "legalEntityExternalId")
    CreateUser toPresentation(User user);
}
