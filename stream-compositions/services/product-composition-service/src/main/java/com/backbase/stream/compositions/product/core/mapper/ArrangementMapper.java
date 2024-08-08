package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ArrangementMapper {
    com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut mapStreamToComposition(
            ArrangementPutItem arrangement);

    ArrangementPutItem mapIntegrationToStream(
        com.backbase.stream.compositions.product.integration.client.model.AccountArrangementItemPut arrangementItem);

    ArrangementPutItem mapCompositionToStream(
            com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut arrangementItem);

    /**
     * The following after mapping has been set because the models are initializing both internalLegalEntities
     * and externalLegalEntities fields, which are Sets, and they need to have at least 1 element. For this scenario
     * if we do not set them as null, the validation rejects them.
     *
     * @param arrangementPutItem {@link ArrangementPutItem} instance to be used for data ingestion
     */
    @AfterMapping
    default void setNullLegalEntities(@MappingTarget ArrangementPutItem arrangementPutItem) {
        arrangementPutItem.setInternalLegalEntities(null);
        arrangementPutItem.setExternalLegalEntities(null);
    }
}
