package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPullEvent;
import com.backbase.stream.compositions.product.client.model.ProductPullIngestionRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ProductCompositionMapper {

    ProductPullEvent map(ProductPullIngestionRequest request);
}
