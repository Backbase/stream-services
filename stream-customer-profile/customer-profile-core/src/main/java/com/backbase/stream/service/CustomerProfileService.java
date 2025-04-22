package com.backbase.stream.service;

import com.backbase.customerprofile.api.service.v1.CustomerManagementServiceApi;
import com.backbase.customerprofile.api.service.v1.model.CustomerCreationRequestDto;
import com.backbase.customerprofile.api.service.v1.model.CustomerResponseDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CustomerProfileService {

    @NonNull
    private final CustomerManagementServiceApi customerManagementServiceApi;

    public Mono<CustomerResponseDto> createCustomer(CustomerCreationRequestDto customerCreationRequestDto)
        throws WebClientResponseException {
        return customerManagementServiceApi.createCustomer(customerCreationRequestDto)
            .doOnError(WebClientResponseException.class, e -> {
                log.error("Error creating customer profile: {}", e.getMessage());
                throw e;
            })
            .doOnSuccess(customerResponseDto ->
                log.info("Customer profile created successfully: {}", customerResponseDto));

    }

}
