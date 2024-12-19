package com.backbase.stream.productcatalog.mapper;


import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementProductItemBase;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementProductsListElement;
import com.backbase.dbs.arrangement.api.service.v3.model.ExternalProductKindItemExtended;
import com.backbase.dbs.arrangement.api.service.v3.model.ProductKindItem;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import org.mapstruct.Mapper;

@Mapper
public interface ProductCatalogMapper {

    ArrangementProductItemBase toPresentation(ProductType productType);

    ExternalProductKindItemExtended toPresentation(ProductKind productKind);

    ProductKind toStream(ProductKindItem productKindItem);

    ProductKind toStream(ExternalProductKindItemExtended presentationProductKindItemGet);

    ProductType toStream(ArrangementProductsListElement arrangementProductsListElement);

}
