package com.backbase.stream.compositions.paymentorders.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class PaymentOrderIngestPullRequest {

    private String memberNumber;
    private String internalUserId;
    private String legalEntityInternalId;
    private String legalEntityExternalId;
    private String serviceAgreementInternalId;
    private Map<String, String> additions;
    private String dateRangeStart;
    private String dateRangeEnd;

}

