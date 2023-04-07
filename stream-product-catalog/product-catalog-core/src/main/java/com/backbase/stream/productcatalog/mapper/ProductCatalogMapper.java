package com.backbase.stream.productcatalog.mapper;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountProductItem;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountSchemasProductItem;
import com.backbase.dbs.arrangement.api.service.v2.model.ExternalProductKindItemExtended;
import com.backbase.dbs.arrangement.api.service.v2.model.ExternalProductKindItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.ProductKindItem;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import org.mapstruct.Mapper;

@Mapper
public interface ProductCatalogMapper {

  AccountProductItem toPresentation(ProductType productType);

  ExternalProductKindItemExtended toPresentation(ProductKind productKind);

  ProductKind toStream(ProductKindItem productKindItem);

  ExternalProductKindItemPut toPutPresentation(ExternalProductKindItemExtended productKindItem);

  ExternalProductKindItemExtended toStream(ExternalProductKindItemPut productKindItem);

  ProductKind toStream(ExternalProductKindItemExtended presentationProductKindItemGet);

  /*ProductKind toStream(ProductKindItem productKindItem);*/

  ProductType toStream(AccountSchemasProductItem productItem);
}
