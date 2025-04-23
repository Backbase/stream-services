package com.backbase.stream.service;

import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PartyUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.mapper.PartyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CustomerProfileService {

    private final PartyManagementIntegrationApi partyManagementIntegrationApi;

    private final PartyMapper partyMapper;

    public Mono<PartyResponseUpsertDto> upsertParty(PartyUpsertDto partyUpsertDto)
        throws WebClientResponseException {
        return partyManagementIntegrationApi.upsertParty(partyUpsertDto)
            .doOnError(WebClientResponseException.class, e -> {
                log.error("Error creating customer profile: {}", e.getMessage());
                throw e;
            })
            .doOnSuccess(customerResponseDto ->
                log.info("Customer profile created successfully: {}", customerResponseDto));
    }

    public Mono<PartyResponseUpsertDto> upsertParty(Party party) {

        var partyUpsertDto = partyMapper.partyToPartyUpsertDto(party);

        return upsertParty(partyUpsertDto);
    }
}
