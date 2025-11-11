package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class InvestmentData {

    private String saName;
    private String saExternalId;
    private List<ClientUser> clientUsers;
    private List<InvestmentArrangement> investmentArrangements;
    private List<InvestorModelPortfolio> portfolioModels;

    public Map<String, List<UUID>> getClientsByLeExternalId() {
        Map<String, List<UUID>> clientsByLeExternalId = new HashMap<>();
        clientUsers.forEach(
            c -> clientsByLeExternalId.computeIfAbsent(c.getLegalEntityExternalId(), l -> new ArrayList<>())
                .add(c.getInvestmentClientId()));
        return clientsByLeExternalId;
    }
}
