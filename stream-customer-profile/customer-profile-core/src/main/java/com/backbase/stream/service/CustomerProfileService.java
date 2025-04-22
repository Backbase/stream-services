package com.backbase.stream.service;

import com.backbase.customerprofile.api.integration.v1.CustomerManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.model.CustomerPartyDto;
import com.backbase.customerprofile.api.integration.v1.model.CustomerResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerManagementIntegrationApi customerManagementIntegrationApi;

    public Mono<CustomerResponseDto> createCustomer(
        CustomerPartyDto customerPartyDto)
        throws WebClientResponseException {
        return customerManagementIntegrationApi.createCustomer(customerPartyDto)
            .doOnError(WebClientResponseException.class, e -> {
                log.error("Error creating customer profile: {}", e.getMessage());
                throw e;
            })
            .doOnSuccess(customerResponseDto ->
                log.info("Customer profile created successfully: {}", customerResponseDto));
    }
}
