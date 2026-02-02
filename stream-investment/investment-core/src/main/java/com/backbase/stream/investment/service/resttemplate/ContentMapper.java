package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import com.backbase.stream.investment.model.MarketNewsEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ContentMapper {

    @Mapping(target = "thumbnail", ignore = true)
    EntryCreateUpdateRequest map(MarketNewsEntry entry);

}
