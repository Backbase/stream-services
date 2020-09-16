package com.backbase.stream.mapper;

import com.backbase.dbs.user.presentation.service.model.AddRealmResponse;
import com.backbase.dbs.user.presentation.service.model.Realm;
import org.mapstruct.Mapper;

@Mapper
public interface RealmMapper {

    Realm toStream(AddRealmResponse realmsPostPostResponseBody);
}
