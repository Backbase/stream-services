package com.backbase.stream.investment;

import com.backbase.stream.investment.model.MarketNewsEntry;
import com.backbase.stream.investment.model.MarketNewsTag;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class InvestmentContentData {

    private List<MarketNewsTag> marketNewsTags;
    private List<MarketNewsEntry> marketNews;

}
