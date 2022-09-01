package com.backbase.stream.compositions.paymentorders.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class PaymentOrderIngestPullRequest {

    private String internalUserId;
    private String legalEntityInternalId;
    private String legalEntityExternalId;
    private String memberNumber;
    private Map<String, String> additions;
    private String dateRangeStart;
    private String dateRangeEnd;

}

