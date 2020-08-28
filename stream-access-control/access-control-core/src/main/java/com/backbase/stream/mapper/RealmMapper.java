package com.backbase.stream.mapper;

import com.backbase.dbs.user.presentation.service.model.Realm;
import com.backbase.dbs.user.presentation.service.model.UsersIdentitiesRealmsPostPostResponseBody;
import org.mapstruct.Mapper;

@Mapper
public interface RealmMapper {

    Realm toStream(UsersIdentitiesRealmsPostPostResponseBody realmsPostPostResponseBody);
}
