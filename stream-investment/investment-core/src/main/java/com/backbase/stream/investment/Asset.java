package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.AssetTypeEnum;
import com.backbase.investment.api.service.v1.model.StatusA10Enum;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Asset implements AssetKey {

    private UUID uuid;
    private String name;
    private String isin;
    private String ticker;
    private StatusA10Enum status;
    private String market;
    private String currency;
    @JsonProperty("extra_data")
    private Map<String, Object> extraData;
    @JsonProperty("asset_type")
    private AssetTypeEnum assetType;
    private List<String> categories;
    private String externalId;
    private String logo;
    private Resource logoFile;
    private String description;
    private Double defaultPrice;

    @Override
    public String getIsin() {
        return isin;
    }

    @Override
    public String getMarket() {
        return market;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

}
