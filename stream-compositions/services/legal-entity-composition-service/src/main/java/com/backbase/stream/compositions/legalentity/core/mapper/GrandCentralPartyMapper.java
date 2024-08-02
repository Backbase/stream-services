package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.gc.party.Email;
import com.backbase.gc.party.Party;
import com.backbase.gc.party.Party.PartyTypeEnum;
import com.backbase.gc.party.PartyDatesInner;
import com.backbase.gc.party.PartyDatesInner.DateTypeEnum;
import com.backbase.gc.party.PhoneAddress;
import com.backbase.stream.compositions.legalentity.core.config.GrandCentralPartyDefaultProperties;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import java.time.OffsetDateTime;
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
    public abstract LegalEntity mapLegalEntity(Party party);

    @Mapping(source = "partyId", target = "externalId")
    @Mapping(constant = "true", target = "isMaster")
    public abstract ServiceAgreement mapServiceAgreement(Party party);

    @Mapping(source = "partyId", target = "externalId")
    public abstract User mapUser(Party party);

    public abstract EmailAddress map(Email email);

    public abstract PhoneNumber map(PhoneAddress phoneAddress);

    @AfterMapping
    protected void updateLegalEntity(Party party, @MappingTarget LegalEntity legalEntity) {
        legalEntity.setName(getPartyName(party));
        legalEntity.setRealmName(properties.getRealmName());
        legalEntity.setParentExternalId(properties.getParentExternalId());
        legalEntity.setMasterServiceAgreement(mapServiceAgreement(party));
        if (PartyTypeEnum.PERSON.equals(party.getPartyType())) {
            legalEntity.setUsers(List.of(new JobProfileUser(mapUser(party))
                .referenceJobRoleNames(properties.getReferenceJobRoleNames())));
        } else if (party.getCustomFields() != null) {
            // If a party is not a person, the custom fields should be applicable for the LE, not the User
            legalEntity.setAdditions(mapCustomFields(party.getCustomFields()));
        }
    }

    @AfterMapping
    protected void updateUser(Party party, @MappingTarget User user) {
        user.setFullName(getPartyName(party));
        user.setIdentityLinkStrategy(properties.getIdentityUserLinkStrategy());
        var email = getPartyEmail(party, Email.TypeEnum.PERSONAL, true)
            .map(this::map)
            .orElseThrow(() -> new RuntimeException("Email is required"));
        user.setEmailAddress(email);
        var mobilePhone = getPartyPhone(party, PhoneAddress.TypeEnum.MOBILE)
            .map(this::map)
            .orElseThrow(() -> new RuntimeException("Mobile phone is required"));
        user.setMobileNumber(mobilePhone);
        if (party.getStatus() != null) {
            user.setLocked(!"ACTIVE".equals(party.getStatus()));
        }
        if (PartyTypeEnum.PERSON.equals(party.getPartyType()) && party.getCustomFields() != null) {
            user.setAdditions(mapCustomFields(party.getCustomFields()));
        }
    }

    @AfterMapping
    protected void updateServiceAgreement(Party party, @MappingTarget ServiceAgreement serviceAgreement) {
        serviceAgreement.setName(getPartyName(party));
        getPartyDate(party, DateTypeEnum.OPENING_DATETIME)
            .map(OffsetDateTime::toLocalDate)
            .ifPresent(serviceAgreement::validFromDate);
        getPartyDate(party, DateTypeEnum.CLOSING_DATETIME)
            .map(OffsetDateTime::toLocalDate)
            .ifPresent(serviceAgreement::validUntilDate);
    }

    protected String getPartyName(Party party) {
        if (PartyTypeEnum.ORGANISATION.equals(party.getPartyType()) && party.getOrganisationName() != null) {
            return party.getOrganisationName();
        }
        return getPersonFullName(party).orElseThrow(() -> new RuntimeException("Party name is required"));
    }

    protected Optional<Email> getPartyEmail(Party party, Email.TypeEnum type, boolean fallbackToFirst) {
        if (party.getElectronicAddress() == null || party.getElectronicAddress().getEmails() == null) {
            return Optional.empty();
        }
        var selectedEmail = party.getElectronicAddress().getEmails().stream()
            .filter(e -> type.equals(e.getType()))
            .findFirst();
        return fallbackToFirst && selectedEmail.isEmpty()
            ? party.getElectronicAddress().getEmails().stream().findFirst()
            : selectedEmail;
    }

    protected Optional<PhoneAddress> getPartyPhone(Party party, PhoneAddress.TypeEnum type) {
        if (party.getPhoneAddresses() == null) {
            return Optional.empty();
        }
        return party.getPhoneAddresses().stream()
            .filter(e -> type.equals(e.getType()))
            .findFirst();
    }

    protected Optional<OffsetDateTime> getPartyDate(Party party, DateTypeEnum dateType) {
        if (party.getDates() == null) {
            return Optional.empty();
        }
        return party.getDates().stream()
            .filter(d -> dateType.equals(d.getDateType()))
            .map(PartyDatesInner::getDateValue)
            .findFirst();
    }

    protected Optional<String> getPersonFullName(Party party) {
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

    private static Map<String, String> mapCustomFields(Map<String, Object> customFields) {
        return customFields.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().toString()
            ));
    }
}


