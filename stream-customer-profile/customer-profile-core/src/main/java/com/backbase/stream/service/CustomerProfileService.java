package com.backbase.stream.service;

import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PartyUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.mapper.PartyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CustomerProfileService {

    private final PartyManagementIntegrationApi partyManagementIntegrationApi;

    private final PartyMapper partyMapper;

    public Mono<PartyResponseUpsertDto> upsertParty(Party party, String legalEntityExternalId) {

        if (legalEntityExternalId != null && !legalEntityExternalId.trim().isEmpty() && party.getIsCustomer()) {
            party.partyId(legalEntityExternalId);
        }
        var partyUpsertDto = partyMapper.partyToPartyUpsertDto(party);

        return partyManagementIntegrationApi.upsertParty(partyUpsertDto);
    }
}
