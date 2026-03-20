package com.backbase.stream.investment;

import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.MarketNewsEntry;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode
@Data
@Builder
@Slf4j
public class InvestmentContentData implements InvestmentDataValue {

    private List<ContentTag> marketNewsTags;
    private List<MarketNewsEntry> marketNews;
    private List<ContentTag> documentTags;
    private List<ContentDocumentEntry> documents;

    public long getTotalProcessedValues() {
        log.debug(
            "Calculating total processed values: marketNewsTags={}, marketNews={}, documentTags={}, documents={}",
            getSize(marketNewsTags), getSize(marketNews), getSize(documentTags), getSize(documents));
        return getSize(marketNewsTags) + getSize(marketNews) + getSize(documentTags) + getSize(documents);
    }
}
