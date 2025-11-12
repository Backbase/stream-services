package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.model.ClientCreate;
import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.OASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class InvestmentClientServiceTest {

    ClientApi clientApi;
    InvestmentClientService service;

    @BeforeEach
    void setUp() {
        clientApi = Mockito.mock(ClientApi.class);
        service = new InvestmentClientService(clientApi);
    }

    @Test
    void createClient_success() {
        ClientCreateRequest request = new ClientCreateRequest();
        ClientCreate created = new ClientCreate(UUID.randomUUID());
        when(clientApi.createClient(any())).thenReturn(Mono.just(created));

    }

    @Test
    void getClient_notFoundReturnsEmpty() {
        UUID uuid = UUID.randomUUID();
        WebClientResponseException notFound = new WebClientResponseException(
            404, "Not Found", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        when(clientApi.getClient(eq(uuid), anyList(), any(), any())).thenReturn(Mono.error(notFound));

        StepVerifier.create(service.getClient(uuid))
            .verifyComplete();
    }

    @Test
    void patchClient_success() {
        UUID uuid = UUID.randomUUID();
        PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest();
        OASClient updated = new OASClient();
        when(clientApi.patchClient(eq(uuid), any())).thenReturn(Mono.just(updated));

        StepVerifier.create(service.patchClient(uuid, patch))
            .expectNext(updated)
            .verifyComplete();
    }

    @Test
    void updateClient_success() {
        UUID uuid = UUID.randomUUID();
        OASClientUpdateRequest update = new OASClientUpdateRequest();
        OASClient updated = new OASClient();
        when(clientApi.updateClient(eq(uuid), any())).thenReturn(Mono.just(updated));

        StepVerifier.create(service.updateClient(uuid, update))
            .expectNext(updated)
            .verifyComplete();
    }
}

