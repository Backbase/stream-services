package com.backbase.stream.compositions.product.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ProductIngestPullRequest {
    private String legalEntityExternalId;
    private Map<String, String> parameters;
}

