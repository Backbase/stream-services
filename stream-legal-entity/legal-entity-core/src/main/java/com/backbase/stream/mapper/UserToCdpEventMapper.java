package com.backbase.stream.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.mapstruct.ReportingPolicy.ERROR;

import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvent;
import com.backbase.cdp.ingestion.api.service.v1.model.CdpEventMetadata;
import com.backbase.cdp.profiles.api.service.v1.model.Address;
import com.backbase.cdp.profiles.api.service.v1.model.CustomerProfile;
import com.backbase.cdp.profiles.api.service.v1.model.DeliveryChannel;
import com.backbase.cdp.profiles.api.service.v1.model.ExternalId;
import com.backbase.cdp.profiles.api.service.v1.model.PersonalInfo;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.Multivalued;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ERROR)
public interface UserToCdpEventMapper {

    String PROFILE_CREATED_EVENT = "ProfileCreatedEvent";

    String SOURCE_BACKBASE = "BACKBASE";
    String SOURCE_CORE_BANKING_SYSTEM = "CORE_SYSTEM";

    String TYPE_USER_ID = "USER_ID";
    String TYPE_CUSTOMER_ID = "CUSTOMER_ID";

    @Mapping(target = "eventId", expression = "java(generateEventId())")
    @Mapping(target = "timestamp", expression = "java(generateEventTimestamp())")
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "cdpCustomerId", ignore = true)
    @Mapping(target = "context", ignore = true)
    @Mapping(target = "metadata", expression = "java(generateEventMetadata(\"stream-services\"))")
    @Mapping(target = "eventType", constant = PROFILE_CREATED_EVENT)
    @Mapping(target = "sourceSystem", constant = SOURCE_BACKBASE)
    @Mapping(target = "sourceType", constant = TYPE_USER_ID)
    @Mapping(target = "sourceId", expression = "java(user.getInternalId())")
    @Mapping(target = "data", expression = "java(mapEntityToData(mapGetUserProfileToCdpCustomerProfile(" +
        "legalEntityInternalId, legalEntityExternalId, customerCategory, user)))")
    CdpEvent mapUserToCdpEvent(String legalEntityInternalId,
                               String legalEntityExternalId,
                               CustomerCategory customerCategory,
                               User user);

    default List<Address> convertPostalAddresses(User user) {
        List<Address> addresses = new ArrayList<>();
        if (nonNull(user.getUserProfile()) && nonNull(user.getUserProfile().getAddresses())) {
            addresses = user.getUserProfile().getAddresses()
                .stream()
                .map(this::mapPostalAddress)
                .toList();
        }
        return addresses;
    }

    default List<DeliveryChannel> convertElectronicAddressesAndPhoneNumbers(User user) {
        List<DeliveryChannel> contactDetails = new ArrayList<>();
        if (nonNull(user.getEmailAddress())) {
            contactDetails.add(mapEmailAddress(user.getEmailAddress()));
        }
        if (nonNull(user.getMobileNumber())) {
            contactDetails.add(mapPhoneAddress(user.getMobileNumber()));
        }
        List<Multivalued> electronicAddresses = new ArrayList<>();
        List<Multivalued> phoneNumbers = new ArrayList<>();
        if (nonNull(user.getUserProfile())) {
            if (nonNull(user.getUserProfile().getAdditionalEmails())) {
                electronicAddresses.addAll(user.getUserProfile().getAdditionalEmails());
            }
            if (nonNull(user.getUserProfile().getAdditionalPhoneNumbers())) {
                phoneNumbers.addAll(user.getUserProfile().getAdditionalPhoneNumbers());
            }
        }
        contactDetails.addAll(electronicAddresses
            .stream().map(this::mapElectronicAddress)
            .toList());
        contactDetails.addAll(phoneNumbers
            .stream().map(this::mapPhoneAddress)
            .toList());
        return contactDetails;
    }

    @Mapping(target = "street1", source = "streetAddress")
    @Mapping(target = "city", source = "locality")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "postalCode", source = "postalCode")
    @Mapping(target = "primary", source = "primary")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "street2", ignore = true)
    @Mapping(target = "state", ignore = true)
    Address mapPostalAddress(com.backbase.stream.legalentity.model.Address postalAddress);

    @Mapping(target = "type",
        expression = "java(com.backbase.cdp.profiles.api.service.v1.model.DeliveryChannel.TypeEnum.EMAIL)")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "primary", source = "primary")
    @Mapping(target = "verified", constant = "true")
    @Mapping(target = "optIn", ignore = true)
    DeliveryChannel mapElectronicAddress(Multivalued electronicAddress);

    @Mapping(target = "type",
        expression = "java(com.backbase.cdp.profiles.api.service.v1.model.DeliveryChannel.TypeEnum.PHONE)")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "primary", source = "primary")
    @Mapping(target = "verified", constant = "true")
    @Mapping(target = "optIn", ignore = true)
    DeliveryChannel mapPhoneAddress(Multivalued phoneAddress);

    @Mapping(target = "firstName", expression = "java(mapFirstName(user))")
    @Mapping(target = "lastName", expression = "java(mapLastName(user))")
    @Mapping(target = "middleName", expression = "java(mapMiddleName(user))")
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "email", expression = "java(getPrimaryEmail(user))")
    @Mapping(target = "phone", expression = "java(getPrimaryPhone(user))")
    @Mapping(target = "address", expression = "java(getPrimaryAddress(convertPostalAddresses(user)))")
    @Mapping(target = "dateOfBirth", expression = "java(mapDateOfBirth(user))")
    @Mapping(target = "gender", expression = "java(mapGender(user))")
    @Mapping(target = "preferredLocale", expression = "java(mapLocale(user))")
    @Mapping(target = "nationality", expression = "java(mapNationality(user))")
    @Mapping(target = "maritalStatus", source = "user.userProfile.personalInformation.maritalStatus")
    @Mapping(target = "occupation", ignore = true)
    @Mapping(target = "employer", source = "user.userProfile.personalInformation.employer")
    @Mapping(target = "employerName", ignore = true)
    PersonalInfo mapPersonalInfo(User user);

    @Mapping(target = "cdpCustomerId", ignore = true)
    @Mapping(target = "externalIds", expression = "java(mapUserToExternalIds(" +
        "user, legalEntityInternalId, legalEntityExternalId))")
    @Mapping(target = "personalInfo", expression = "java(mapPersonalInfo(user))")
    @Mapping(target = "deliveryChannels", expression = "java(convertElectronicAddressesAndPhoneNumbers(user))")
    @Mapping(target = "productHoldings", ignore = true)
    @Mapping(target = "financialSummary", ignore = true)
    @Mapping(target = "behavioralMetrics", ignore = true)
    @Mapping(target = "inferredAttributes", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "complianceInfo", ignore = true)
    @Mapping(target = "addresses", expression = "java(convertPostalAddresses(user))")
    @Mapping(target = "segments", ignore = true)
    @Mapping(target = "segmentCodes", ignore = true)
    @Mapping(target = "organizationInfo", ignore = true)
    @Mapping(target = "profileExtendedData", ignore = true)
    @Mapping(target = "cardProductHoldings", ignore = true)
    @Mapping(target = "profileStatus", expression = "java(mapUserStatus(user))")
    @Mapping(target = "profileType", expression = "java(mapProfileType(customerCategory))")
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", constant = "0L")
    CustomerProfile mapGetUserProfileToCdpCustomerProfile(String legalEntityInternalId,
                                                          String legalEntityExternalId,
                                                          CustomerCategory customerCategory,
                                                          User user);

    default CustomerProfile.ProfileTypeEnum mapProfileType(CustomerCategory customerCategory) {
        if (isNull(customerCategory)) {
            return CustomerProfile.ProfileTypeEnum.PERSON;
        }
        return customerCategory.equals(CustomerCategory.RETAIL)
            ? CustomerProfile.ProfileTypeEnum.PERSON
            : CustomerProfile.ProfileTypeEnum.EMPLOYEE;
    }

    default String mapUserStatus(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getActive())) {
            return "ACTIVE";
        }
        return Boolean.TRUE.equals(user.getUserProfile().getActive()) ? "ACTIVE" : "INACTIVE";
    }

    default String mapNationality(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getPersonalInformation())
            || isNull(user.getUserProfile().getPersonalInformation().getNationalities())
            || user.getUserProfile().getPersonalInformation().getNationalities().isEmpty()) {
            return null;
        }
        return user.getUserProfile().getPersonalInformation().getNationalities().getFirst();
    }

    default String getPrimaryEmail(User user) {
        if (nonNull(user.getEmailAddress())) {
            return user.getEmailAddress().getAddress();
        } else if (nonNull(user.getUserProfile()) && nonNull(user.getUserProfile().getAdditionalEmails())) {
            return getPrimaryValueFromMultiValued(user.getUserProfile().getAdditionalEmails());
        }
        return null;
    }

    default String getPrimaryPhone(User user) {
        if (nonNull(user.getMobileNumber())) {
            return user.getMobileNumber().getNumber();
        } else if (nonNull(user.getUserProfile()) && nonNull(user.getUserProfile().getAdditionalPhoneNumbers())) {
            return getPrimaryValueFromMultiValued(user.getUserProfile().getAdditionalPhoneNumbers());
        }
        return null;
    }

    default String getPrimaryValueFromMultiValued(List<Multivalued> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        return addresses.stream()
            .filter(address -> address.getPrimary() != null && address.getPrimary())
            .findFirst()
            .orElse(addresses.getFirst())
            .getValue();
    }

    default PersonalInfo.GenderEnum mapGender(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getPersonalInformation())
            || isNull(user.getUserProfile().getPersonalInformation().getGender())
            || user.getUserProfile().getPersonalInformation().getGender().isEmpty()) {
            return null;
        }
        return mapStringToGender(user.getUserProfile().getPersonalInformation().getGender());
    }

    default String mapLocale(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getLocale())) {
            return null;
        }
        return user.getUserProfile().getLocale();
    }

    default String mapFirstName(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getName())
            || isNull(user.getUserProfile().getName().getGivenName())) {
            return null;
        }
        return user.getUserProfile().getName().getGivenName();
    }

    default String mapLastName(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getName())
            || isNull(user.getUserProfile().getName().getFamilyName())) {
            return null;
        }
        return user.getUserProfile().getName().getFamilyName();
    }

    default String mapMiddleName(User user) {
        if (isNull(user)
            || isNull(user.getUserProfile())
            || isNull(user.getUserProfile().getName())
            || isNull(user.getUserProfile().getName().getMiddleName())) {
            return null;
        }
        return user.getUserProfile().getName().getMiddleName();
    }

    default LocalDate mapDateOfBirth(User user) {
        if (isNull(user) || isNull(user.getUserProfile()) || isNull(user.getUserProfile().getPersonalInformation())
        || isNull(user.getUserProfile().getPersonalInformation().getDateOfBirth())) {
            return null;
        }
        String dateOfBirth = user.getUserProfile().getPersonalInformation().getDateOfBirth();
        if (isNull(dateOfBirth) || dateOfBirth.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateOfBirth);
    }

    @Mapping(target = "type",
        expression = "java(com.backbase.cdp.profiles.api.service.v1.model.DeliveryChannel.TypeEnum.PHONE)")
    @Mapping(target = "value", source = "number")
    @Mapping(target = "primary", source = "primary")
    @Mapping(target = "verified", constant = "true")
    @Mapping(target = "optIn", ignore = true)
    DeliveryChannel mapPhoneAddress(PhoneNumber phoneAddress);

    @Mapping(target = "type",
        expression = "java(com.backbase.cdp.profiles.api.service.v1.model.DeliveryChannel.TypeEnum.EMAIL)")
    @Mapping(target = "value", source = "address")
    @Mapping(target = "primary", source = "primary")
    @Mapping(target = "verified", constant = "true")
    @Mapping(target = "optIn", ignore = true)
    DeliveryChannel mapEmailAddress(EmailAddress emailAddress);

    default Address getPrimaryAddress(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        return addresses.stream()
            .filter(address -> address.getPrimary() != null && address.getPrimary())
            .findFirst()
            .orElse(addresses.getFirst());
    }

    default PersonalInfo.GenderEnum mapStringToGender(String gender) {
        if (gender == null || gender.isEmpty()) {
            return null;
        }
        try {
            return PersonalInfo.GenderEnum.fromValue(gender.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    default Map<String, Object> mapEntityToData(Object entity) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.convertValue(entity, new TypeReference<>() {});
    }

    default String generateEventId() {
        return java.util.UUID.randomUUID().toString();
    }

    default OffsetDateTime generateEventTimestamp() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    default CdpEventMetadata generateEventMetadata(String eventSource) {
        return new CdpEventMetadata()
            .processedBy(List.of("stream-services"))
            .schemaVersion("1")
            .source(eventSource);
    }

    default List<ExternalId> mapUserToExternalIds(User user, String legalEntityInternalId, String legalEntityExternalId) {
        List<ExternalId> externalIds = new ArrayList<>();
        externalIds.add(new ExternalId()
            .source(SOURCE_CORE_BANKING_SYSTEM)
            .type(TYPE_USER_ID)
            .id(user.getExternalId())
            .verified(true));
        if (nonNull(user.getInternalId())) {
            externalIds.add(new ExternalId()
                .source(SOURCE_BACKBASE)
                .type(TYPE_USER_ID)
                .id(user.getInternalId())
                .verified(true));
        }
        if (nonNull(legalEntityInternalId)) {
            externalIds.add(new ExternalId()
                .source(SOURCE_BACKBASE)
                .type(TYPE_CUSTOMER_ID)
                .id(legalEntityInternalId)
                .verified(true));
        }
        if (nonNull(legalEntityExternalId)) {
            externalIds.add(new ExternalId()
                .source(SOURCE_CORE_BANKING_SYSTEM)
                .type(TYPE_CUSTOMER_ID)
                .id(legalEntityExternalId)
                .verified(true));
        }
        return externalIds;
    }

}
