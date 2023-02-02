package com.backbase.stream.compositions.product.core.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ArrangementMapper {
    com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut mapStreamToComposition(
            com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut arrangement);

    com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut mapIntegrationToStream(
            com.backbase.stream.compositions.integration.product.model.AccountArrangementItemPut arrangementItem);

    com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut mapCompositionToStream(
            com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut arrangementItem);
}
