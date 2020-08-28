package com.backbase.stream.mapper;

import com.backbase.dbs.user.presentation.service.model.UserCreateItem;
import com.backbase.dbs.user.presentation.service.model.UserItem;
import com.backbase.stream.legalentity.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {


    @Mapping(source = "id", target = "internalId")
    User toStream(UserItem userItem);

    @Mapping(source = "legalEntityId", target = "legalEntityExternalId")
    UserCreateItem toPresentation(User user);
}
