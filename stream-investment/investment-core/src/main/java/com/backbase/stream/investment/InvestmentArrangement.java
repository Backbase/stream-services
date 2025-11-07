package com.backbase.stream.investment;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentArrangement {

    private String name;
    private String externalId;
    private String internalId;
    private String internalUserId;
    private String externalUserId;
    private String productTypeExternalId;
    private String currency;

    private UUID investmentProductId;
    private List<String> legalEntityExternalIds;

}
