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

    public Mono<PartyResponseUpsertDto> upsertParty(PartyUpsertDto partyUpsertDto) {
        return partyManagementIntegrationApi.upsertParty(partyUpsertDto);
    }

    public Mono<PartyResponseUpsertDto> upsertParty(Party party) {

        var partyUpsertDto = partyMapper.partyToPartyUpsertDto(party);

        return upsertParty(partyUpsertDto);
    }
}
