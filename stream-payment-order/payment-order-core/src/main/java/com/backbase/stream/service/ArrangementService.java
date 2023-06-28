package com.backbase.stream.service;


import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage Products (In DBS Called Arrangements).
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class ArrangementService {

    private final ArrangementsApi arrangementsApi;

    public ArrangementService(ArrangementsApi arrangementsApi) {
        this.arrangementsApi = arrangementsApi;
    }

    public String getArrangementInternalId(String externalId) {

        return arrangementsApi.getInternalId(externalId).map(accountInternalIdGetResponseBody
            -> accountInternalIdGetResponseBody.getInternalId()).block();
    }
}
