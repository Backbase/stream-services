package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.grandcentral.event.spec.v1.Data;
import com.backbase.grandcentral.event.spec.v1.Email;
import com.backbase.grandcentral.event.spec.v1.PhoneAddress;
import com.backbase.stream.compositions.legalentity.core.config.GrandCentralPartyDefaultProperties;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.User;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Mapper(componentModel = "spring")
@EnableConfigurationProperties(GrandCentralPartyDefaultProperties.class)
public abstract class GrandCentralPartyMapper {

    @Autowired
    private GrandCentralPartyDefaultProperties properties;

    @Mapping(source = "partyId", target = "externalId")
    @Mapping(constant = "CUSTOMER", target = "legalEntityType")
    public abstract LegalEntity mapLegalEntity(Data party);

    @Mapping(source = "partyId", target = "externalId")
    public abstract User mapUser(Data party);

    public abstract EmailAddress map(Email email);

    public abstract PhoneNumber map(PhoneAddress phoneAddress);

    @AfterMapping
    protected void updateLegalEntity(Data party, @MappingTarget LegalEntity legalEntity) {
        if (properties.getParentExternalId() == null
            || properties.getReferenceJobRoleNames() == null
            || properties.getReferenceJobRoleNames().isEmpty()) {
            throw new RuntimeException("Default party parent external id or reference job roles are empty");
        }

        legalEntity.setName(getPartyName(party));
        legalEntity.setRealmName(properties.getRealmName());
        legalEntity.setParentExternalId(properties.getParentExternalId());
        if (Data.PartyType.PERSON.equals(party.getPartyType())) {
            legalEntity.setUsers(List.of(new JobProfileUser(mapUser(party))
                .referenceJobRoleNames(properties.getReferenceJobRoleNames())));
        } else if (party.getCustomFields() != null) {
            // If a party is not a person, the custom fields should be applicable for the LE, not the User
            legalEntity.setAdditions(mapCustomFields(party.getCustomFields()));
        }
    }

    @AfterMapping
    protected void updateUser(Data party, @MappingTarget User user) {
        user.setFullName(getPartyName(party));
        user.setIdentityLinkStrategy(properties.getIdentityUserLinkStrategy());
        var email = getPartyEmail(party, Email.Type.PERSONAL)
            .map(this::map)
            .orElseThrow(() -> new RuntimeException("Party email is required"));
        user.setEmailAddress(email);
        var mobilePhone = getPartyPhone(party, PhoneAddress.Type.MOBILE)
            .map(this::map)
            .orElseThrow(() -> new RuntimeException("Party phone is required"));
        user.setMobileNumber(mobilePhone);
        if (party.getStatus() != null) {
            user.setLocked(!"ACTIVE".equals(party.getStatus()));
        }
        if (Data.PartyType.PERSON.equals(party.getPartyType()) && party.getCustomFields() != null) {
            user.setAdditions(mapCustomFields(party.getCustomFields()));
        }
    }

    private String getPartyName(Data party) {
        if (Data.PartyType.ORGANISATION.equals(party.getPartyType()) && party.getOrganisationName() != null) {
            return party.getOrganisationName();
        }
        return getPersonFullName(party).orElseThrow(() -> new RuntimeException("Party name is required"));
    }

    private Optional<Email> getPartyEmail(Data party, Email.Type preferredType) {
        if (party.getElectronicAddress() == null || party.getElectronicAddress().getEmails() == null) {
            return Optional.empty();
        }
        return party.getElectronicAddress().getEmails().stream()
            .filter(e -> preferredType.equals(e.getType()))
            .findFirst()
            .or(party.getElectronicAddress().getEmails().stream()::findFirst);
    }

    private Optional<PhoneAddress> getPartyPhone(Data party, PhoneAddress.Type preferredType) {
        if (party.getPhoneAddresses() == null) {
            return Optional.empty();
        }
        return party.getPhoneAddresses().stream()
            .filter(e -> preferredType.equals(e.getType()))
            .findFirst()
            .or(party.getPhoneAddresses().stream()::findFirst);
    }

    private Optional<String> getPersonFullName(Data party) {
        if (party.getPersonName() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Stream.of(
                party.getPersonName().getFirstName(),
                party.getPersonName().getMiddleName(),
                party.getPersonName().getFamilyName())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" ")));
    }

    private Map<String, String> mapCustomFields(Map<String, Object> customFields) {
        return customFields.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().toString()
            ));
    }

}


