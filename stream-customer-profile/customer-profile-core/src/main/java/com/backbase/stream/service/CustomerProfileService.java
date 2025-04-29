package com.backbase.stream.service;

import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.mapper.PartyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CustomerProfileService {

    private final PartyManagementIntegrationApi partyManagementIntegrationApi;

    private final PartyMapper partyMapper;

    public Mono<PartyResponseUpsertDto> upsertParty(Party party, String legalEntityInternalId) {
        var partyUpsertDto = partyMapper.partyToPartyUpsertDto(party);
        if (StringUtils.hasText(legalEntityInternalId) && party.getIsCustomer()) {
            partyUpsertDto.setLegalEntityId(legalEntityInternalId);
        }
        return partyManagementIntegrationApi.upsertParty(partyUpsertDto);
    }
}
