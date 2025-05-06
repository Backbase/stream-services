package com.backbase.stream.mapper;


import static com.backbase.stream.FixtureUtils.reflectiveAlphaFixtureMonkey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.stream.legalentity.model.Party;
import com.navercorp.fixturemonkey.FixtureMonkey;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PartyMapperImpl.class)
class MapperTest {

    @Autowired
    private PartyMapper partyMapper;
    private final FixtureMonkey fixtureMonkey = reflectiveAlphaFixtureMonkey;

    @Test
    @DisplayName("Should map basic fields correctly when not null")
    void shouldMapBasicFields() {
        var party = fixtureMonkey.giveMeOne(Party.class);

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNotNull(resultDto);
        assertEquals(party.getPartyId(), resultDto.getPartyId());
        assertEquals(party.getIsCustomer(), resultDto.getIsCustomer());
        assertEquals(party.getPreferredLanguage(), resultDto.getPreferredLanguage());
        assertEquals(party.getNotes(), resultDto.getNotes());
        assertEquals(party.getClosingDateTime(), resultDto.getClosingDateTime());
        assertEquals(party.getApprovedDateTime(), resultDto.getApprovedDateTime());
        assertEquals(party.getLastUpdatedDateTime(), resultDto.getLastUpdatedDateTime());
        assertEquals(party.getOpeningDateTime(), resultDto.getOpeningDateTime());
        assertEquals(party.getLiveDateTime(), resultDto.getLiveDateTime());

        assertNotNull(resultDto.getPartyType());
        assertEquals(party.getPartyType().getValue(), resultDto.getPartyType().getValue());
        assertNotNull(resultDto.getState());
        assertEquals(party.getState().getValue(), resultDto.getState().getValue());
        assertNotNull(resultDto.getSubState());
        assertEquals(party.getSubState().getValue(), resultDto.getSubState().getValue());
    }

    @Test
    @DisplayName("Should map Person when PartyType is PERSON and Person is not null")
    void shouldMapPersonWhenPartyTypeIsPerson() {
        var party = fixtureMonkey.giveMeBuilder(Party.class)
            .set("partyType", Party.PartyTypeEnum.PERSON)
            .setNotNull("person")
            .setNotNull("person.personName")
            .set("person.personName.firstName", "John")
            .set("person.personName.familyName", "Doe")
            .setNull("organisation")
            .sample();

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNotNull(resultDto.getPerson());
        assertEquals("John", resultDto.getPerson().getPersonName().getFirstName());
        assertEquals("Doe", resultDto.getPerson().getPersonName().getFamilyName());
        if (party.getPerson().getIdentifications() != null) {
            assertNotNull(resultDto.getPerson().getIdentifications());
            assertEquals(party.getPerson().getIdentifications().size(),
                resultDto.getPerson().getIdentifications().size());
        }
        assertNull(resultDto.getOrganisation(), "Organisation should be null if PartyType is PERSON");
    }

    @Test
    @DisplayName("Should handle null Person from source")
    void shouldHandleNullPerson() {
        var party = fixtureMonkey.giveMeBuilder(Party.class)
            .setNull("person")
            .sample();

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNull(resultDto.getPerson());
    }

    @Test
    @DisplayName("Should map Organisation when PartyType is ORGANISATION and Organisation is not null")
    void shouldMapOrganisationWhenPartyTypeIsOrganisation() {
        var party = fixtureMonkey.giveMeBuilder(Party.class)
            .set("partyType", Party.PartyTypeEnum.ORGANISATION)
            .setNotNull("organisation")
            .set("organisation.name", "My Company")
            .setNull("person")
            .sample();
        var resultDto = partyMapper.partyToPartyUpsertDto(party);
        assertNotNull(resultDto.getOrganisation());
        assertEquals("My Company", resultDto.getOrganisation().getName());
        assertNull(resultDto.getPerson());
    }

    @Test
    @DisplayName("Should handle null Organisation from source")
    void shouldHandleNullOrganisation() {
        Party party = fixtureMonkey.giveMeBuilder(Party.class)
            .setNull("organisation")
            .sample();
        var resultDto = partyMapper.partyToPartyUpsertDto(party);
        assertNull(resultDto.getOrganisation());
    }

    @Test
    @DisplayName("Should map populated collections correctly")
    void shouldMapPopulatedCollections() {

        var party = fixtureMonkey.giveMeBuilder(Party.class)
            .size("phoneNumbers", 2)
            .size("postalAddresses", 1)
            .size("customFields", 3)
            .size("partyPartyRelationships", 1)
            .sample();

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNotNull(resultDto.getPhoneNumbers());
        assertEquals(2, resultDto.getPhoneNumbers().size());

        assertNotNull(resultDto.getPostalAddresses());
        assertEquals(1, resultDto.getPostalAddresses().size());

        assertNotNull(resultDto.getAdditions());
        assertEquals(3, resultDto.getAdditions().size());

        assertNotNull(resultDto.getPartyPartyRelationships());
        assertEquals(1, resultDto.getPartyPartyRelationships().size());
    }

    @Test
    @DisplayName("Should map empty collections correctly")
    void shouldMapEmptyCollections() {
        var party = fixtureMonkey.giveMeBuilder(Party.class)
            .set("phoneNumbers", new ArrayList<>())
            .set("postalAddresses", new ArrayList<>())
            .set("customFields", new HashMap<>())
            .set("partyPartyRelationships", new ArrayList<>())
            .sample();

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNotNull(resultDto.getPhoneNumbers());
        assertTrue(resultDto.getPhoneNumbers().isEmpty());

        assertNotNull(resultDto.getPostalAddresses());
        assertTrue(resultDto.getPostalAddresses().isEmpty());

        assertNotNull(resultDto.getAdditions());
        assertTrue(resultDto.getAdditions().isEmpty());

        assertNotNull(resultDto.getPartyPartyRelationships());
        assertTrue(resultDto.getPartyPartyRelationships().isEmpty());
    }

    @Test
    @DisplayName("Should map empty collections correctly")
    void shouldMapNullCollections() {

        var party = fixtureMonkey.giveMeBuilder(Party.class)
            .setNull("phoneNumbers")
            .setNull("postalAddresses")
            .setNull("customFields")
            .setNull("partyPartyRelationships")
            .setNull("electronicAddresses.emails")
            .setNull("electronicAddresses.urls")
            .setNull("person.identifications")
            .setNull("organisation.identifications")
            .sample();

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNotNull(resultDto.getPhoneNumbers());
        assertTrue(resultDto.getPhoneNumbers().isEmpty());

        assertNotNull(resultDto.getPostalAddresses());
        assertTrue(resultDto.getPostalAddresses().isEmpty());

        assertNotNull(resultDto.getAdditions());
        assertTrue(resultDto.getAdditions().isEmpty());

        assertNotNull(resultDto.getPartyPartyRelationships());
        assertTrue(resultDto.getPartyPartyRelationships().isEmpty());

        assertNotNull(resultDto.getElectronicAddresses());
        assertNotNull(resultDto.getElectronicAddresses().getEmails());
        assertTrue(resultDto.getElectronicAddresses().getEmails().isEmpty());

        assertNotNull(resultDto.getElectronicAddresses().getUrls());
        assertTrue(resultDto.getElectronicAddresses().getUrls().isEmpty());
    }

}
