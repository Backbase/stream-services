package com.backbase.stream.investment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record AssetLatestPrice(@JsonProperty("uuid") UUID uuid,
                               @JsonProperty("latest_price") LatestPrice latestPrice) {}
