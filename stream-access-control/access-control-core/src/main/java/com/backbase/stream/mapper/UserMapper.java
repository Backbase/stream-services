package com.backbase.stream.mapper;

import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.User;
import com.backbase.dbs.user.api.service.v2.model.UserExternal;
import com.backbase.identity.integration.api.service.v1.model.EnhancedUserRepresentation;
import com.backbase.identity.integration.api.service.v1.model.UserRequestBody;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

  @Mapping(source = "id", target = "internalId")
  com.backbase.stream.legalentity.model.User toStream(GetUser userItem);

  User toService(com.backbase.stream.legalentity.model.User legalEntityUser);

  @Mapping(source = "legalEntityId", target = "legalEntityExternalId")
  UserExternal toPresentation(com.backbase.stream.legalentity.model.User user);

  UserRequestBody toPresentation(EnhancedUserRepresentation user);

  @Mapping(source = "externalId", target = "externalId")
  @Mapping(source = "legalEntityId", target = "legalEntityId")
  @Mapping(source = "userProfile.preferredLanguage", target = "preferredLanguage")
  @Mapping(source = "additions", target = "additions")
  User toServiceUser(com.backbase.stream.legalentity.model.User user);
}
