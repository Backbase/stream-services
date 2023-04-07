package com.backbase.stream.compositions.productcatalog.core.model;

import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductCatalogIngestResponse {
  private ProductCatalog productCatalog;
}
