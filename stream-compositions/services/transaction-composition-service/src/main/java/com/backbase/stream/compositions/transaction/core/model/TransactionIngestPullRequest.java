package com.backbase.stream.compositions.transaction.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestPullRequest {
    private String externalArrangementIds;
    private Map<String, String> additionalParameters;
}

