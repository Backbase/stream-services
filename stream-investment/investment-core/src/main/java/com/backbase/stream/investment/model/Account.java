package com.backbase.stream.investment.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Account {
    private String accountExternalId;
    private Boolean isDefault;
    private Boolean isInternal;
    private String productTypeExternalId;
}
