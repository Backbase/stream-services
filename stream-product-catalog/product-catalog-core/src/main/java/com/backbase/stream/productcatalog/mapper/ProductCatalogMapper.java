package com.backbase.stream.productcatalog.mapper;

import com.backbase.dbs.accounts.presentation.service.model.*;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ProductCatalogMapper {

    ProductItem toPresentation(ProductType productType);

    ProductKindItem toPresentation(ProductKind productKind);

    ProductKindItemPut toPutPresentation(ProductKindItem productKindItem);

    ProductKindItem toStream(ProductKindItemPut productKindItem);

    ProductKind toStream(PresentationProductKindItemGet presentationProductKindItemGet);

    ProductKind toStream(ProductKindItem productKindItem);

    ProductType toStream(SchemasProductItem schemasProductItem);


}
