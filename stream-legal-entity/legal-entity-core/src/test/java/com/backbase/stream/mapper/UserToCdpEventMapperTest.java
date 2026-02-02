package com.backbase.stream.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvent;
import com.backbase.cdp.profiles.api.service.v1.model.CustomerProfile;
import com.backbase.cdp.profiles.api.service.v1.model.ExternalId;
import com.backbase.stream.legalentity.model.Address;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.Multivalued;
import com.backbase.stream.legalentity.model.PersonalInformation;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserToCdpEventMapperTest {

    private UserToCdpEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserToCdpEventMapper.class);
    }

    @Test
    void testMapUserToCdpEvent_basicFields() {
        User user = new User();
        user.setInternalId("int-1");
        user.setExternalId("ext-1");
        user.setEmailAddress(new EmailAddress().address("test@email.com").primary(true));
        user.setMobileNumber(new PhoneNumber().number("123456789").primary(true));
        user.setUserProfile(new UserProfile());
        CustomerCategory category = CustomerCategory.RETAIL;
        CdpEvent event = mapper.mapUserToCdpEvent("leIntId", "leExtId", category, user);
        assertThat(event).isNotNull();
        assertThat(event.getEventType()).isEqualTo(UserToCdpEventMapper.PROFILE_CREATED_EVENT);
        assertThat(event.getSourceSystem()).isEqualTo(UserToCdpEventMapper.SOURCE_BACKBASE);
        assertThat(event.getSourceType()).isEqualTo(UserToCdpEventMapper.TYPE_USER_ID);
        assertThat(event.getSourceId()).isEqualTo("int-1");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getMetadata()).isNotNull();
        assertThat(event.getMetadata().getProcessedBy()).isNotNull().contains("stream-services");
        assertThat(event.getMetadata().getSchemaVersion()).isEqualTo("1");
        assertThat(event.getMetadata().getSource()).isEqualTo("stream-services");
        assertThat(event.getData()).isNotNull();
    }

    @Test
    void testConvertPostalAddresses_handlesNulls() {
        User user = new User();
        assertThat(mapper.convertPostalAddresses(user)).isNotNull().isEmpty();
    }

    @Test
    void testGetPrimaryValueFromMultiValued_prefersPrimary() {
        Multivalued m1 = new Multivalued().value("a").primary(false);
        Multivalued m2 = new Multivalued().value("b").primary(true);
        List<Multivalued> list = List.of(m1, m2);
        assertThat(mapper.getPrimaryValueFromMultiValued(list)).isEqualTo("b");
    }

    @Test
    void shouldProperlyMapProfileTypeTest() {
        CustomerProfile.ProfileTypeEnum profileType = mapper.mapProfileType(CustomerCategory.BUSINESS);
        assertThat(profileType).isEqualTo(CustomerProfile.ProfileTypeEnum.EMPLOYEE);
    }

    @Test
    void testGetPrimaryValueFromMultiValued_fallbackToFirst() {
        Multivalued m1 = new Multivalued().value("a").primary(false);
        Multivalued m2 = new Multivalued().value("b").primary(false);
        List<Multivalued> list = List.of(m1, m2);
        assertThat(mapper.getPrimaryValueFromMultiValued(list)).isEqualTo("a");
    }

    @Test
    void testMapUserStatus_nulls() {
        assertThat(mapper.mapUserStatus(null)).isEqualTo("ACTIVE");
        User user = new User();
        assertThat(mapper.mapUserStatus(user)).isEqualTo("ACTIVE");
        user.setUserProfile(new UserProfile());
        assertThat(mapper.mapUserStatus(user)).isEqualTo("ACTIVE");
    }

    @Test
    void testMapUserStatus_inactive() {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setActive(false);
        user.setUserProfile(profile);
        assertThat(mapper.mapUserStatus(user)).isEqualTo("INACTIVE");
    }

    @Test
    void testMapDateOfBirth_handlesNullsAndEmpty() {
        User user = new User();
        assertThat(mapper.mapDateOfBirth(user)).isNull();
        UserProfile profile = new UserProfile();
        user.setUserProfile(profile);
        assertThat(mapper.mapDateOfBirth(user)).isNull();
        profile.setPersonalInformation(new PersonalInformation());
        assertThat(mapper.mapDateOfBirth(user));
        profile.getPersonalInformation().setDateOfBirth("");
        assertThat(mapper.mapDateOfBirth(user)).isNull();
    }

    @Test
    void testMapDateOfBirth_valid() {
        User user = new User();
        UserProfile profile = new UserProfile();
        PersonalInformation pi = new PersonalInformation();
        pi.setDateOfBirth("2000-01-01");
        profile.setPersonalInformation(pi);
        user.setUserProfile(profile);
        assertThat(mapper.mapDateOfBirth(user)).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    void testMapUserToExternalIds_allFields() {
        User user = new User();
        user.setInternalId("int");
        user.setExternalId("ext");
        List<ExternalId> ids = mapper.mapUserToExternalIds(user, "leInt", "leExt");
        assertThat(ids).hasSize(4);
    }

    @Test
    void testMapUserToExternalIds_handlesNulls() {
        User user = new User();
        List<ExternalId> ids = mapper.mapUserToExternalIds(user, null, null);
        assertThat(ids).isEmpty();
        user.setInternalId("int");
        ids = mapper.mapUserToExternalIds(user, null, null);
        assertThat(ids).hasSize(1);
    }

    @Test
    void testPhoneAddressMultiValuedMapping() {
        Multivalued phone = new Multivalued().value("+123456789").primary(false);
        var deliveryChannel = mapper.mapPhoneAddress(phone);
        assertThat(deliveryChannel).isNotNull();
        assertThat(deliveryChannel.getPrimary()).isFalse();
        assertThat(deliveryChannel.getValue()).isEqualTo("+123456789");
    }

    @Test
    void testElectronicAddressMapping() {
        Multivalued email = new Multivalued().value("test@address.com").primary(true);
        var deliveryChannel = mapper.mapElectronicAddress(email);
        assertThat(deliveryChannel).isNotNull();
        assertThat(deliveryChannel.getPrimary()).isTrue();
        assertThat(deliveryChannel.getValue()).isEqualTo("test@address.com");
    }

    @Test
    void testPrimaryEmailMapping() {
        EmailAddress email = new EmailAddress().address("test@address.com").primary(true);
        var deliveryChannel = mapper.mapEmailAddress(email);
        assertThat(deliveryChannel).isNotNull();
        assertThat(deliveryChannel.getPrimary()).isTrue();
        assertThat(deliveryChannel.getValue()).isEqualTo("test@address.com");
    }

    @Test
    void testPhoneAddressMapping() {
        PhoneNumber phone = new PhoneNumber().number("987654321").primary(true);
        var deliveryChannel = mapper.mapPhoneAddress(phone);
        assertThat(deliveryChannel).isNotNull();
        assertThat(deliveryChannel.getPrimary()).isTrue();
        assertThat(deliveryChannel.getValue()).isEqualTo("987654321");
    }

    @Test
    void testPostalAddressMappingWhenNull() {
        User user = new User();
        var addresses = mapper.convertPostalAddresses(user);
        assertThat(addresses).isNotNull().isEmpty();
    }

    @Test
    void testPostalAddressMapping() {
        UserProfile userProfile = new UserProfile();
        userProfile.setAddresses(List.of(new Address()
            .streetAddress("123 Main St")
            .locality("Anytown")
            .region("CA")
            .postalCode("12345")
            .country("USA")
            .primary(true)
        , new Address()
            .streetAddress("124 Main St")
            .locality("Sometown")
            .region("Alberta")
            .postalCode("54321")
            .country("Canada")
            .primary(true)
        ));
        User user = new User();
        user.setUserProfile(userProfile);
        var addresses = mapper.convertPostalAddresses(user);
        assertThat(addresses).isNotNull().hasSize(2);
        assertThat(addresses.stream().map(com.backbase.cdp.profiles.api.service.v1.model.Address::getStreet1))
            .containsExactlyInAnyOrder("123 Main St", "124 Main St");
    }
}
