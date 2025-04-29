package com.backbase.stream.service;

import static com.backbase.stream.FixtureUtils.reflectiveAlphaFixtureMonkey;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PartyUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.mapper.PartyMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    private CustomerProfileService customerProfileService;

    private final FixtureMonkey fixtureMonkey = reflectiveAlphaFixtureMonkey;
    @Mock
    private PartyManagementIntegrationApi partyManagementIntegrationApiMock;


    @BeforeEach
    void setup() {
        customerProfileService = new CustomerProfileService(partyManagementIntegrationApiMock, Mappers.getMapper(PartyMapper.class));
    }

    @Test
    @DisplayName("Upsert party should return PartyResponseUpsertDto when API call is successful")
    void createCustomer_success() {
        var legalEntityId = UUID.randomUUID().toString();
        var requestDto = fixtureMonkey.giveMeBuilder(Party.class)
            .set("legalEntityId", legalEntityId)
            .sample();
        var expectedResponseDto = fixtureMonkey.giveMeBuilder(PartyResponseUpsertDto.class)
            .set("partyReferenceId", legalEntityId).sample();

        when(partyManagementIntegrationApiMock.upsertParty(any(PartyUpsertDto.class)))
            .thenReturn(Mono.just(expectedResponseDto));

        var result = customerProfileService.upsertParty(requestDto, legalEntityId);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertNotNull(response);
                assertNotNull(response.getCustomerReferenceId());
                assertNotNull(response.getPartyReferenceId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Upsert party should propagate WebClientResponseException when API call fails")
    void upsertParty_apiError() {
        var requestDto = fixtureMonkey.giveMeOne(Party.class);
        var expectedException = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request from API",
            null,
            null,
            StandardCharsets.UTF_8);
        when(partyManagementIntegrationApiMock.upsertParty(any(PartyUpsertDto.class)))
            .thenReturn(Mono.error(expectedException));
        var result = customerProfileService.upsertParty(requestDto, null);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                    throwable.getMessage().contains("Bad Request from API")
            )
            .verify();
    }

    @Test
    @DisplayName("Upsert party should propagate other RuntimeExceptions when API call fails unexpectedly")
    void upsertParty_otherError() {
        var requestDto = fixtureMonkey.giveMeOne(Party.class);
        var expectedException = new RuntimeException("Unexpected error");
        when(partyManagementIntegrationApiMock.upsertParty(any(PartyUpsertDto.class)))
            .thenReturn(Mono.error(expectedException));
        var result = customerProfileService.upsertParty(requestDto, null);
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable == expectedException)
            .verify();
    }
}
