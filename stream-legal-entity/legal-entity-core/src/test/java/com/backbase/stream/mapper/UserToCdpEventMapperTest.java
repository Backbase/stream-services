package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvent;
import com.backbase.cdp.profiles.api.service.v1.model.ExternalId;
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
        assertNotNull(event);
        assertEquals(UserToCdpEventMapper.PROFILE_CREATED_EVENT, event.getEventType());
        assertEquals(UserToCdpEventMapper.SOURCE_BACKBASE, event.getSourceSystem());
        assertEquals(UserToCdpEventMapper.TYPE_USER_ID, event.getSourceType());
        assertEquals("int-1", event.getSourceId());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getMetadata());
        assertNotNull(event.getMetadata().getProcessedBy());
        assertTrue(event.getMetadata().getProcessedBy().contains("stream-services"));
        assertEquals("1", event.getMetadata().getSchemaVersion());
        assertEquals("stream-services", event.getMetadata().getSource());
        assertNotNull(event.getData());
    }

    @Test
    void testConvertPostalAddresses_handlesNulls() {
        User user = new User();
        assertNotNull(mapper.convertPostalAddresses(user));
        assertTrue(mapper.convertPostalAddresses(user).isEmpty());
    }

    @Test
    void testGetPrimaryValueFromMultiValued_prefersPrimary() {
        Multivalued m1 = new Multivalued().value("a").primary(false);
        Multivalued m2 = new Multivalued().value("b").primary(true);
        List<Multivalued> list = List.of(m1, m2);
        assertEquals("b", mapper.getPrimaryValueFromMultiValued(list));
    }

    @Test
    void testGetPrimaryValueFromMultiValued_fallbackToFirst() {
        Multivalued m1 = new Multivalued().value("a").primary(false);
        Multivalued m2 = new Multivalued().value("b").primary(false);
        List<Multivalued> list = List.of(m1, m2);
        assertEquals("a", mapper.getPrimaryValueFromMultiValued(list));
    }

    @Test
    void testMapUserStatus_nulls() {
        assertEquals("ACTIVE", mapper.mapUserStatus(null));
        User user = new User();
        assertEquals("ACTIVE", mapper.mapUserStatus(user));
        user.setUserProfile(new UserProfile());
        assertEquals("ACTIVE", mapper.mapUserStatus(user));
    }

    @Test
    void testMapUserStatus_inactive() {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setActive(false);
        user.setUserProfile(profile);
        assertEquals("INACTIVE", mapper.mapUserStatus(user));
    }

    @Test
    void testMapDateOfBirth_handlesNullsAndEmpty() {
        User user = new User();
        assertNull(mapper.mapDateOfBirth(user));
        UserProfile profile = new UserProfile();
        user.setUserProfile(profile);
        assertNull(mapper.mapDateOfBirth(user));
        profile.setPersonalInformation(new PersonalInformation());
        assertNull(mapper.mapDateOfBirth(user));
        profile.getPersonalInformation().setDateOfBirth("");
        assertNull(mapper.mapDateOfBirth(user));
    }

    @Test
    void testMapDateOfBirth_valid() {
        User user = new User();
        UserProfile profile = new UserProfile();
        PersonalInformation pi = new PersonalInformation();
        pi.setDateOfBirth("2000-01-01");
        profile.setPersonalInformation(pi);
        user.setUserProfile(profile);
        assertEquals(LocalDate.of(2000, 1, 1), mapper.mapDateOfBirth(user));
    }

    @Test
    void testMapUserToExternalIds_allFields() {
        User user = new User();
        user.setInternalId("int");
        user.setExternalId("ext");
        List<ExternalId> ids = mapper.mapUserToExternalIds(user, "leInt", "leExt");
        assertEquals(4, ids.size());
    }

    @Test
    void testMapUserToExternalIds_handlesNulls() {
        User user = new User();
        List<ExternalId> ids = mapper.mapUserToExternalIds(user, null, null);
        assertTrue(ids.isEmpty());
        user.setInternalId("int");
        ids = mapper.mapUserToExternalIds(user, null, null);
        assertEquals(1, ids.size());
    }

    // Add more tests for edge cases and custom logic as needed
}
