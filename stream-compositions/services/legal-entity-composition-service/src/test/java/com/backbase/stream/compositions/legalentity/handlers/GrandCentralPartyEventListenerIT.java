package com.backbase.stream.compositions.legalentity.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.grandcentral.event.spec.v1.PartyUpsertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
@AutoConfigureWireMock(port = 0)
public class GrandCentralPartyEventListenerIT {

    @Autowired
    private GrandCentralPartyEventListener listener;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void reset() {
        WireMock.resetAllRequests();
    }

    @Test
    void processPersonPartyUpsertEventTest() throws IOException {
        var resource = new ClassPathResource("events/person-party-upsert-event.json");
        PartyUpsertEvent event = objectMapper.readValue(resource.getInputStream(), PartyUpsertEvent.class);
        var envelopedEvent = new EnvelopedEvent<PartyUpsertEvent>();
        envelopedEvent.setEvent(event);
        listener.handle(envelopedEvent);

        verify(postRequestedFor(urlEqualTo("/service-api/v3/accesscontrol/legal-entities/create")));
        verify(postRequestedFor(urlEqualTo("/service-api/v3/accesscontrol/service-agreements/ingest")));
        verify(putRequestedFor(urlEqualTo("/service-api/v3/accessgroups/users/permissions/user-permissions")));
        verify(postRequestedFor(urlEqualTo("/service-api/v2/users/identities")));
    }

    @Test
    void processOrganizationPartyUpsertEventTest() throws IOException {
        var resource = new ClassPathResource("events/organization-party-upsert-event.json");
        PartyUpsertEvent event = objectMapper.readValue(resource.getInputStream(), PartyUpsertEvent.class);
        var envelopedEvent = new EnvelopedEvent<PartyUpsertEvent>();
        envelopedEvent.setEvent(event);
        listener.handle(envelopedEvent);

        verify(postRequestedFor(urlEqualTo("/service-api/v3/accesscontrol/legal-entities/create")));
        verify(postRequestedFor(urlEqualTo("/service-api/v3/accesscontrol/service-agreements/ingest")));
        verify(0, putRequestedFor(urlEqualTo("/service-api/v3/accessgroups/users/permissions/user-permissions")));
        verify(0, postRequestedFor(urlEqualTo("/service-api/v2/users/identities")));
    }

    @Test
    void processPartyUpsertEventWithMissingDataTest() throws IOException {
        var resource = new ClassPathResource("events/person-party-upsert-event.json");
        PartyUpsertEvent event = objectMapper.readValue(resource.getInputStream(), PartyUpsertEvent.class);
        event.getData().setElectronicAddress(null);
        event.getData().setPhoneAddresses(List.of());
        var envelopedEvent = new EnvelopedEvent<PartyUpsertEvent>();
        envelopedEvent.setEvent(event);
        assertThrows(RuntimeException.class,
            () -> listener.handle(envelopedEvent),
            "Should throw runtime exception when required fields are missing");
    }

}
