package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ModelPortfolio {

    private UUID uuid;
    @JsonAlias("external_id")
    private String externalId;
    private String arrangementExternalId;
    private ProductTypeEnum productTypeEnum;
    private String name;
    @JsonAlias("cash_weight")
    private double cashWeight;
    @JsonAlias("risk_level")
    private int riskLevel;
    @JsonAlias("allocation")
    private List<Allocation> allocations;
    private UUID createdFor;
    private Map<String, String> extraData;

    public void uuid(UUID uuid) {
        this.uuid = uuid;
    }

}
