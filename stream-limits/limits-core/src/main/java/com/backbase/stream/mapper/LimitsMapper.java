package com.backbase.stream.mapper;

import static java.util.Collections.singletonList;

import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.LimitKey;
import com.backbase.dbs.limit.api.service.v2.model.LimitsRetrievalPostRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.UpdateLimitRequestBody;

import org.mapstruct.Mapper;

@Mapper
public interface LimitsMapper {

    default LimitsRetrievalPostRequestBody map(CreateLimitRequestBody req) {
        return (LimitsRetrievalPostRequestBody)
                new LimitsRetrievalPostRequestBody()
                        .limitsRetrievalOptions(
                                singletonList(
                                        new LimitKey()
                                                .lookupKeys(req.getEntities())
                                                .userBBID(req.getUserBBID())
                                                .shadow(req.getShadow())));
    }

    UpdateLimitRequestBody mapUpdateLimits(CreateLimitRequestBody item);
}
