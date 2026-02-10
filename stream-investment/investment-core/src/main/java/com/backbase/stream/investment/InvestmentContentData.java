package com.backbase.stream.investment;

import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.MarketNewsEntry;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class InvestmentContentData {

    private List<ContentTag> marketNewsTags;
    private List<MarketNewsEntry> marketNews;
    private List<ContentTag> documentTags;
    private List<ContentDocumentEntry> documents;

}
