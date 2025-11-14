package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import org.mapstruct.Mapper;

@Mapper
public interface InvestmentMapper {

    PatchedOASClientUpdateRequest map(OASClient client);

}
