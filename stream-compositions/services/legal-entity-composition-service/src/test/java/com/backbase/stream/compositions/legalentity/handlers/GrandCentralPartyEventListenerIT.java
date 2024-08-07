package com.backbase.stream.compositions.legalentity.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.grandcentral.event.spec.v1.PartyEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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

    @Test
    void processPartyEventTest() throws IOException {
        var resource = new ClassPathResource("events/party-event.json");
        PartyEvent event = objectMapper.readValue(resource.getInputStream(), PartyEvent.class);
        var envelopedEvent = new EnvelopedEvent<PartyEvent>();
        envelopedEvent.setEvent(event);
        listener.handle(envelopedEvent);

        verify(postRequestedFor(urlEqualTo("/service-api/v2/users/identities")));
        verify(postRequestedFor(urlEqualTo("/service-api/v3/accesscontrol/legal-entities/create")));
        verify(postRequestedFor(urlEqualTo("/service-api/v3/accesscontrol/service-agreements/ingest")));
        verify(putRequestedFor(urlEqualTo("/service-api/v3/accessgroups/users/permissions/user-permissions")));
    }

}
