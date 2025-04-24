package com.backbase.stream.mapper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.backbase.stream.legalentity.model.Party;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.jqwik.JavaTypeArbitraryGenerator;
import com.navercorp.fixturemonkey.api.jqwik.JqwikPlugin;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.arbitraries.StringArbitrary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {
    PartyMapperImpl.class,
    PersonMapperImpl.class,
    OrganisationMapperImpl.class,
    PartyRelationshipMapperImpl.class,
    AddressMapperImpl.class,
    ElectronicAddressMapperImpl.class,
    IdentificationMapperImpl.class,
    DemographicsMapperImpl.class
})
public class MapperTest {

    @Autowired
    private PartyMapper partyMapper;
    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
        .defaultNotNull(true)
        .plugin(new JqwikPlugin().javaTypeArbitraryGenerator(new JavaTypeArbitraryGenerator() {
            @Override
            public StringArbitrary strings() {
                return Arbitraries.strings().alpha();
            }
        }))
        .build();

    @Test
    void testPartyToPartyUpsertDtoMapping() {

        var party = fixtureMonkey.giveMeOne(Party.class);

        assertNotNull(party.getPartyId(), "FixtureMonkey should generate a partyId");
        assertNotNull(party.getIsCustomer(), "FixtureMonkey should generate isCustomer");
        assertNotNull(party.getPartyType(), "FixtureMonkey should generate partyType");

        var resultDto = partyMapper.partyToPartyUpsertDto(party);

        assertNotNull(resultDto);

        assertEquals(party.getPartyId(), resultDto.getPartyId());
        assertEquals(party.getIsCustomer(), resultDto.getIsCustomer());

        if (party.getPartyType() != null) {
            assertNotNull(resultDto.getPartyType());
            assertEquals(party.getPartyType().getValue(), resultDto.getPartyType().getValue()); //
        } else {
            assertNull(resultDto.getPartyType());
        }
        if (party.getState() != null) {
            assertNotNull(resultDto.getState());
            assertEquals(party.getState().getValue(), resultDto.getState().getValue()); //
        } else {
            assertNull(resultDto.getState());
        }
        assertEquals(party.getClosingDateTime(), resultDto.getClosingDateTime()); //
        assertEquals(party.getApprovedDateTime(), resultDto.getApprovedDateTime()); //
        assertEquals(party.getLastUpdatedDateTime(), resultDto.getLastUpdatedDateTime()); //
        assertEquals(party.getOpeningDateTime(), resultDto.getOpeningDateTime()); //
        assertEquals(party.getLiveDateTime(), resultDto.getLiveDateTime()); //
        assertEquals(party.getPreferredLanguage(), resultDto.getPreferredLanguage()); //
        assertEquals(party.getNotes(), resultDto.getNotes()); //

        assertNull(resultDto.getLegalEntityId(), "legalEntityId debe ser ignorado por el mapper");
        assertNull(resultDto.getSubState(), "subState debe ser ignorado por el mapper");

        if (party.getCustomFields() != null) {
            assertNotNull(resultDto.getAdditions());
            assertEquals(party.getCustomFields().size(), resultDto.getAdditions().size());
            assertEquals(party.getCustomFields(), resultDto.getAdditions());
        }

        if (party.getPhoneAddresses() != null) {
            assertNotNull(resultDto.getPhoneNumbers());
            assertEquals(party.getPhoneAddresses().size(), resultDto.getPhoneNumbers().size());

            if (!party.getPhoneAddresses().isEmpty()) {
                assertEquals(party.getPhoneAddresses().get(0).getNumber(),
                    resultDto.getPhoneNumbers().get(0).getNumber());
            }
        } else {
            assertNull(resultDto.getPhoneNumbers());
        }

        if (party.getElectronicAddress() != null) {
            assertNotNull(resultDto.getElectronicAddresses());
            if (party.getElectronicAddress().getEmails() != null) {
                assertNotNull(resultDto.getElectronicAddresses().getEmails());
                assertEquals(party.getElectronicAddress().getEmails().size(),
                    resultDto.getElectronicAddresses().getEmails().size());
            } else {
                assertNull(resultDto.getElectronicAddresses().getEmails());
            }
            if (party.getElectronicAddress().getUrls() != null) {
                assertNotNull(resultDto.getElectronicAddresses().getUrls());
                assertEquals(party.getElectronicAddress().getUrls().size(),
                    resultDto.getElectronicAddresses().getUrls().size());
            } else {
                assertNull(resultDto.getElectronicAddresses().getUrls());
            }
        } else {
            assertNull(resultDto.getElectronicAddresses());
        }

        if (party.getPerson() != null) {
            assertNotNull(resultDto.getPerson());

            assertEquals(party.getPerson().getPersonName().getFirstName(),
                resultDto.getPerson().getPersonName().getFirstName());

            assertEquals(party.getPerson().getPersonName().getFamilyName(),
                resultDto.getPerson().getPersonName().getFamilyName());
            if (party.getPerson().getIdentifications() != null) {
                assertNotNull(resultDto.getPerson().getIdentifications());
                assertEquals(party.getPerson().getIdentifications().size(),
                    resultDto.getPerson().getIdentifications().size());
            } else {
                assertNull(resultDto.getPerson().getIdentifications());
            }
            if (party.getPerson().getDemographics() != null) {
                assertNotNull(resultDto.getPerson().getDemographics());
            } else {
                assertNull(resultDto.getPerson().getDemographics());
            }

        } else {
            if (party.getPartyType() != Party.PartyTypeEnum.PERSON) {
                assertNull(resultDto.getPerson());
            } else {
                assertNull(resultDto.getPerson());
            }
        }

        if (party.getOrganisation() != null) {
            assertNotNull(resultDto.getOrganisation());
            assertEquals(party.getOrganisation().getName(), resultDto.getOrganisation().getName());
            if (party.getOrganisation().getIdentifications() != null) {
                assertNotNull(resultDto.getOrganisation().getIdentifications());
                assertEquals(party.getOrganisation().getIdentifications().size(),
                    resultDto.getOrganisation().getIdentifications().size());
            } else {
                assertNull(resultDto.getOrganisation().getIdentifications());
            }
            if (party.getOrganisation().getLegalStructure() != null) {
                assertNotNull(resultDto.getOrganisation().getLegalStructure());
            } else {
                assertNull(resultDto.getOrganisation().getLegalStructure());
            }
        } else {
            if (party.getPartyType() != Party.PartyTypeEnum.ORGANISATION) {
                assertNull(resultDto.getOrganisation());
            } else {
                assertNull(resultDto.getOrganisation());
            }
        }

        if (party.getPostalAddresses() != null) {
            assertNotNull(resultDto.getPostalAddresses());
            assertEquals(party.getPostalAddresses().size(), resultDto.getPostalAddresses().size());
        } else {
            assertNull(resultDto.getPostalAddresses());
        }

        if (party.getPartyPartyRelationships() != null) {
            assertNotNull(resultDto.getPartyPartyRelationships());
            assertEquals(party.getPartyPartyRelationships().size(), resultDto.getPartyPartyRelationships().size());
        } else {
            assertNull(resultDto.getPartyPartyRelationships());
        }

    }
}
