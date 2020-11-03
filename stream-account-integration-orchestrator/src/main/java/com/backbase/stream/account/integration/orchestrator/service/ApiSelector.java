package com.backbase.stream.account.integration.orchestrator.service;

import com.backbase.stream.dbs.account.integration.outbound.api.ArrangementDetailsApi;
import com.backbase.stream.dbs.account.integration.outbound.api.ArrangementsApi;
import com.backbase.stream.dbs.account.integration.outbound.api.BalancesApi;
import com.backbase.stream.dbs.account.integration.outbound.api.RecipientArrangementIdsApi;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApiSelector {

    private final BalancesApi defaultApi;
    private final ArrangementDetailsApi defaultArrangementDetailsApi;
    private final ArrangementsApi defaultArrangementsApi;
    private final RecipientArrangementIdsApi defaultRecipientArrangementIdsApi;

    private final Map<String, BalancesApi> balancesApiMap;
    private final Map<String, ArrangementDetailsApi> arrangementDetailsApiMap;
    private final Map<String, ArrangementsApi> arrangementsApiMap;
    private final Map<String, RecipientArrangementIdsApi> recipientArrangementIdsApiMap;

    public ApiSelector(BalancesApi defaultApi,
                       ArrangementDetailsApi defaultArrangementDetailsApi,
                       ArrangementsApi arrangementsApi,
                       RecipientArrangementIdsApi recipientArrangementIdsApi,
                       @Qualifier("productTypeBalancesApi") Map<String, BalancesApi> balancesApiMap,
                       @Qualifier("productTypeArrangementDetailsApi") Map<String, ArrangementDetailsApi> arrangementDetailsApiMap,
                       @Qualifier("productTypeArrangementsApi") Map<String, ArrangementsApi> arrangementsApiMap,
                       @Qualifier("productTypeRecipientArrangementIdsApi") Map<String, RecipientArrangementIdsApi> recipientArrangementIdsApiMap) {
        this.defaultApi = defaultApi;
        this.defaultArrangementDetailsApi = defaultArrangementDetailsApi;
        this.defaultArrangementsApi = arrangementsApi;
        this.defaultRecipientArrangementIdsApi = recipientArrangementIdsApi;
        this.balancesApiMap = balancesApiMap;
        this.arrangementDetailsApiMap = arrangementDetailsApiMap;
        this.arrangementsApiMap = arrangementsApiMap;
        this.recipientArrangementIdsApiMap = recipientArrangementIdsApiMap;
    }

    public BalancesApi balancesApi(String productType) {
        BalancesApi balancesApi = balancesApiMap.getOrDefault(productType, defaultApi);
        log.info("Selected BalanceAPI for ProductType: {} {}", productType, balancesApi.getApiClient().getBasePath());
        return balancesApi;
    }


    public ArrangementDetailsApi arrangementDetailsApi(String productType) {
        ArrangementDetailsApi orDefault = arrangementDetailsApiMap.getOrDefault(productType, defaultArrangementDetailsApi);
        log.info("Selected BalanceAPI for ProductType: {} {}", productType, orDefault.getApiClient().getBasePath());
        return orDefault;
    }


    public ArrangementsApi arrangementsApi(String productType) {
        ArrangementsApi orDefault = arrangementsApiMap.getOrDefault(productType, defaultArrangementsApi);
        log.info("Selected BalanceAPI for ProductType: {} {}", productType, orDefault.getApiClient().getBasePath());
        return orDefault;
    }

    public RecipientArrangementIdsApi recipientArrangementIdsApi(String productType) {
        RecipientArrangementIdsApi orDefault = recipientArrangementIdsApiMap.getOrDefault(productType, defaultRecipientArrangementIdsApi);
        log.info("Selected BalanceAPI for ProductType: {} {}", productType, orDefault.getApiClient().getBasePath());
        return orDefault;
    }


}
