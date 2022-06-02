package com.backbase.stream.compositions.transaction.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestPullRequest {
    private String externalArrangementId;
    private Map<String, String> additionalParameters;
    private OffsetDateTime dateRangeStart;
    private OffsetDateTime dateRangeEnd;
    private Integer billingCycles;
}

