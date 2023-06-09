package com.backbase.stream.compositions.transaction.core.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TransactionIngestPullRequest {

    private String externalArrangementId;
    private String legalEntityInternalId;
    private String arrangementId;
    private Map<String, String> additions;
    private OffsetDateTime dateRangeStart;
    private OffsetDateTime dateRangeEnd;
    private Integer billingCycles;

    private List<String> lastIngestedExternalIds;
}
