package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ModelPortfolioTemplate {

    private UUID uuid;
    private String arrangementExternalId;
    private ProductTypeEnum productTypeEnum;
    private String name;
    private double cashWeight;
    private int riskLevel;
    private List<Allocation> allocations;

    public void uuid(UUID uuid) {
        this.uuid = uuid;
    }

}
