package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.model.ClientCreate;
import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.OASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.PaginatedOASClientList;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import com.backbase.stream.investment.ClientUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Nested
    @DisplayName("upsertClients")
    class UpsertClientsTests {

        @Test
        @DisplayName("single client fails in batch — remaining clients still processed")
        void upsertClients_singleFailure_doesNotStopBatch() {
            String failUserId = "user-fail";
            String okUserId = "user-ok";
            UUID createdUuid = UUID.randomUUID();

            when(clientApi.listClients(anyList(), isNull(), isNull(), isNull(), isNull(),
                eq(failUserId), isNull(), eq(1), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedOASClientList().results(List.of())));
            when(clientApi.createClient(argThat(r -> r != null && failUserId.equals(r.getInternalUserId()))))
                .thenReturn(Mono.error(new RuntimeException("API failure for client 1")));

            OASClient created = Mockito.mock(OASClient.class);
            when(created.getUuid()).thenReturn(createdUuid);
            when(created.getInternalUserId()).thenReturn(okUserId);
            when(created.getExtraData()).thenReturn(java.util.Map.of("user_external_id", "ext-ok"));

            when(clientApi.listClients(anyList(), isNull(), isNull(), isNull(), isNull(),
                eq(okUserId), isNull(), eq(1), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedOASClientList().results(List.of())));
            ClientCreate clientCreate = new ClientCreate(createdUuid)
                .internalUserId(okUserId)
                .putExtraDataItem("user_external_id", "ext-ok");
            when(clientApi.createClient(argThat(r -> r != null && okUserId.equals(r.getInternalUserId()))))
                .thenReturn(Mono.just(clientCreate));

            List<ClientUser> input = List.of(
                ClientUser.builder()
                    .internalUserId(failUserId)
                    .externalUserId("ext-fail")
                    .legalEntityId("le-1")
                    .build(),
                ClientUser.builder()
                    .internalUserId(okUserId)
                    .externalUserId("ext-ok")
                    .legalEntityId("le-2")
                    .build()
            );

            StepVerifier.create(service.upsertClients(input))
                .assertNext(clients -> {
                    assertThat(clients).hasSize(1);
                    assertThat(clients.getFirst().getInternalUserId()).isEqualTo(okUserId);
                    assertThat(clients.getFirst().getInvestmentClientId()).isEqualTo(createdUuid);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("all clients fail — returns empty list without throwing")
        void upsertClients_allFail_returnsEmptyList() {
            String failUserId = "user-all-fail";

            when(clientApi.listClients(anyList(), isNull(), isNull(), isNull(), isNull(),
                eq(failUserId), isNull(), eq(1), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedOASClientList().results(List.of())));
            when(clientApi.createClient(any()))
                .thenReturn(Mono.error(new RuntimeException("API failure")));

            List<ClientUser> input = List.of(
                ClientUser.builder()
                    .internalUserId(failUserId)
                    .externalUserId("ext-fail")
                    .legalEntityId("le-1")
                    .build()
            );

            StepVerifier.create(service.upsertClients(input))
                .assertNext(List::isEmpty)
                .verifyComplete();
        }
    }
}

