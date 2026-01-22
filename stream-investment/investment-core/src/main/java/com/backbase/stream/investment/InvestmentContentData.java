package com.backbase.stream.investment;

import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class InvestmentContentData {

    private List<EntryCreateUpdateRequest> marketNews;

}
