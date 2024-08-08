package com.backbase.stream.compositions.product.core.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ArrangementMapper {
    com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut mapStreamToComposition(
            com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem arrangement);

    com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem mapIntegrationToStream(
        com.backbase.stream.compositions.product.integration.client.model.AccountArrangementItemPut arrangementItem);

    com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem mapCompositionToStream(
            com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut arrangementItem);
}
