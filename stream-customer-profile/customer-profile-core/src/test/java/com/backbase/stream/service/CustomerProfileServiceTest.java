package com.backbase.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.customerprofile.api.service.v1.CustomerManagementServiceApi;
import com.backbase.customerprofile.api.service.v1.model.CustomerCreationRequestDto;
import com.backbase.customerprofile.api.service.v1.model.CustomerResponseDto;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    private CustomerProfileService customerProfileService;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
        .build();
    @Mock
    private CustomerManagementServiceApi customerManagementServiceApiMock;


    @BeforeEach
    void setup() {
        customerProfileService = new CustomerProfileService(customerManagementServiceApiMock);
    }

    @Test
    @DisplayName("createCustomer should return CustomerResponseDto when API call is successful")
    void createCustomer_success() {
        var legalEntityId = UUID.randomUUID().toString();
        var requestDto = fixtureMonkey.giveMeBuilder(CustomerCreationRequestDto.class)
            .set("legalEntityId", legalEntityId)
            .sample();
        var expectedResponseDto = fixtureMonkey.giveMeBuilder(CustomerResponseDto.class)
            .set("legalEntityId", legalEntityId).sample();

        when(customerManagementServiceApiMock.createCustomer(any(CustomerCreationRequestDto.class)))
            .thenReturn(Mono.just(expectedResponseDto));

        var result = customerProfileService.createCustomer(requestDto);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(expectedResponseDto.getLegalEntityId(), response.getLegalEntityId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("createCustomer should propagate WebClientResponseException when API call fails")
    void createCustomer_apiError() {
        var requestDto = fixtureMonkey.giveMeOne(CustomerCreationRequestDto.class);
        var expectedException = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request from API",
            null,
            null,
            StandardCharsets.UTF_8);
        when(customerManagementServiceApiMock.createCustomer(any(CustomerCreationRequestDto.class)))
            .thenReturn(Mono.error(expectedException));
        var result = customerProfileService.createCustomer(requestDto);
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                    throwable instanceof WebClientResponseException &&
                        ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        throwable.getMessage().contains("Bad Request from API")
            )
            .verify();
    }

    @Test
    @DisplayName("createCustomer should propagate other RuntimeExceptions when API call fails unexpectedly")
    void createCustomer_otherError() {
        var requestDto = fixtureMonkey.giveMeOne(CustomerCreationRequestDto.class);
        var expectedException = new RuntimeException("Unexpected error");
        when(customerManagementServiceApiMock.createCustomer(any(CustomerCreationRequestDto.class)))
            .thenReturn(Mono.error(expectedException));
        var result = customerProfileService.createCustomer(requestDto);
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable == expectedException)
            .verify();
    }
}
