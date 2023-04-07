package com.backbase.stream.mapper;

import com.backbase.dbs.user.api.service.v2.model.AddRealmResponse;
import com.backbase.dbs.user.api.service.v2.model.Realm;

import org.mapstruct.Mapper;

@Mapper
public interface RealmMapper {

    Realm toStream(AddRealmResponse realmsPostPostResponseBody);
}
