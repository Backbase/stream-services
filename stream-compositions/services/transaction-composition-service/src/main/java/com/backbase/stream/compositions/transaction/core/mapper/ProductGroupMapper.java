package com.backbase.stream.compositions.transaction.core.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ProductGroupMapper {
    com.backbase.stream.compositions.integration.transaction.model.ProductGroup mapCompositionToIntegration(
            com.backbase.stream.compositions.transaction.model.ProductGroup productGroup);
}
