package com.backbase.stream.compositions.productcatalog.core.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductCatalogIngestPullRequest {

  private Map<String, String> additionalParameters;
}
