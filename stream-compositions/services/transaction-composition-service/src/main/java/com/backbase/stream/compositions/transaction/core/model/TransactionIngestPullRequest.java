package com.backbase.stream.compositions.transaction.core.model;

import com.backbase.stream.compositions.transaction.model.ProductGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestPullRequest {
    private String userExternalId;
    private ProductGroup productGroup;
    private Map<String, String> additionalParameters;
}

