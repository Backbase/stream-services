package com.backbase.stream.investment.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class PaginatedExpandedAssetList {

    public static final String JSON_PROPERTY_COUNT = "count";
    private Integer count;

    public static final String JSON_PROPERTY_NEXT = "next";
    private URI next;

    public static final String JSON_PROPERTY_PREVIOUS = "previous";
    private URI previous;

    public static final String JSON_PROPERTY_RESULTS = "results";
    private List<AssetWithMarketAndLatestPrice> results = new ArrayList<>();

}
