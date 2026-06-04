package com.backbase.stream.investment;

import static com.backbase.investment.api.service.v1.model.PortfolioProduct.JSON_PROPERTY_ADVICE_ENGINE;
import static com.backbase.investment.api.service.v1.model.PortfolioProduct.JSON_PROPERTY_EXTERNAL_ID;
import static com.backbase.investment.api.service.v1.model.PortfolioProduct.JSON_PROPERTY_EXTRA_DATA;
import static com.backbase.investment.api.service.v1.model.PortfolioProduct.JSON_PROPERTY_MODEL_PORTFOLIO;
import static com.backbase.investment.api.service.v1.model.PortfolioProduct.JSON_PROPERTY_PRODUCT_CATEGORY;
import static com.backbase.investment.api.service.v1.model.PortfolioProduct.JSON_PROPERTY_PRODUCT_TYPE;

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
    @JsonProperty(JSON_PROPERTY_EXTERNAL_ID)
    private String externalId;
    private PortfolioProductStatusEnum status = PortfolioProductStatusEnum.ACTIVE;
    @JsonProperty(JSON_PROPERTY_PRODUCT_CATEGORY)
    private String productCategory;
    private UUID uuid;
    @JsonProperty(JSON_PROPERTY_ADVICE_ENGINE)
    private String adviceEngine;
    @JsonProperty(JSON_PROPERTY_MODEL_PORTFOLIO)
    private InvestorModelPortfolio modelPortfolio;
    @JsonProperty(JSON_PROPERTY_PRODUCT_TYPE)
    private ProductTypeEnum productType;
    @JsonProperty(JSON_PROPERTY_EXTRA_DATA)
    private Map<String, String> extraData = new HashMap<>();

}
