package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PortfolioProductBadge;
import com.backbase.investment.api.service.v1.model.PortfolioProductStatusEnum;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;

/**
 * Lightweight projection of {@link com.backbase.investment.api.service.v1.model.Asset} that keeps the DTO immutable
 * while providing helpers to translate to/from the generated model.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductPortfolio {

    private String name;
    private String description;
    private String image;
    private Resource imageResource;
    private Integer order;
    private PortfolioProductBadge badge;
    @JsonProperty("external_id")
    private String externalId;
    private PortfolioProductStatusEnum status = PortfolioProductStatusEnum.ACTIVE;
    @JsonProperty("product_category")
    private String productCategory;
    private UUID uuid;
    @JsonProperty("advice_engine")
    private String adviceEngine;
    @JsonProperty("model_portfolio")
    private InvestorModelPortfolio modelPortfolio;
    @JsonProperty("product_type")
    private ProductTypeEnum productType;
    @JsonProperty("extra_data")
    private Map<String, String> extraData = new HashMap<>();

}
