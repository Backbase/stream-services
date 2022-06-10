package com.backbase.stream.compositions.transaction.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class TransactionIngestPullRequest {
    private String externalArrangementId;
    private String legalEntityInternalId;
    private String arrangementId;
    private Map<String, String> additionalParameters;
    private OffsetDateTime dateRangeStart;
    private OffsetDateTime dateRangeEnd;
    private Integer billingCycles;

    private List<String> lastIngestedExternalIds;
}

