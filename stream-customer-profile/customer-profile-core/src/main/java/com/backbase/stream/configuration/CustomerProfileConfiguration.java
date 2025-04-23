package com.backbase.stream.configuration;

import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.stream.clients.config.CustomerProfileClientConfig;
import com.backbase.stream.mapper.AddressMapper;
import com.backbase.stream.mapper.DemographicsMapper;
import com.backbase.stream.mapper.ElectronicAddressMapper;
import com.backbase.stream.mapper.IdentificationMapper;
import com.backbase.stream.mapper.OrganisationMapper;
import com.backbase.stream.mapper.PartyMapper;
import com.backbase.stream.mapper.PartyRelationshipMapper;
import com.backbase.stream.mapper.PersonMapper;
import com.backbase.stream.service.CustomerProfileService;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(CustomerProfileClientConfig.class)
public class CustomerProfileConfiguration {


    @Bean
    public PartyMapper partyMapper() {
        return Mappers.getMapper(PartyMapper.class);
    }

    @Bean
    public AddressMapper addressMapper() {
        return Mappers.getMapper(AddressMapper.class);
    }

    @Bean
    public IdentificationMapper identificationMapper() {
        return Mappers.getMapper(IdentificationMapper.class);
    }

    @Bean
    public DemographicsMapper demographicsMapper() {
        return Mappers.getMapper(DemographicsMapper.class);
    }

    @Bean
    public PersonMapper personMapper() {
        return Mappers.getMapper(PersonMapper.class);
    }

    @Bean
    public OrganisationMapper organisationMapper() {
        return Mappers.getMapper(OrganisationMapper.class);
    }

    @Bean
    public PartyRelationshipMapper partyRelationshipMapper() {
        return Mappers.getMapper(PartyRelationshipMapper.class);
    }

    @Bean
    public ElectronicAddressMapper electronicAddressMapper() {
        return Mappers.getMapper(ElectronicAddressMapper.class);
    }

    @Bean
    public CustomerProfileService createCustomerProfileService(
        PartyManagementIntegrationApi partyManagementIntegrationApi, PartyMapper partyMapper) {
        return new CustomerProfileService(partyManagementIntegrationApi, partyMapper);
    }
}
