package com.backbase.stream.compositions.productcatalog.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ProductCatalogIngestPullRequest {
    private Map<String, String> additionalParameters;
}
