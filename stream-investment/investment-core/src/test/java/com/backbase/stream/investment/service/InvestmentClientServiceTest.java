package com.backbase.stream.investment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.model.ClientCreate;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("InvestmentClientService")
class InvestmentClientServiceTest {

    private ClientApi clientApi;
    private InvestmentClientService service;

    @BeforeEach
    void setUp() {
        clientApi = mock(ClientApi.class);
        service = new InvestmentClientService(clientApi);
    }

    @Nested
    @DisplayName("upsertClients")
    class UpsertClientsTests {

        @Test
        @DisplayName("no existing client — creates new client")
        void noExistingClient_createsNewClient() {
            String internalUserId = "internal-1";
            String externalUserId = "external-1";
            String legalEntityId = "le-1";
            UUID clientUuid = UUID.randomUUID();

            ClientUser clientUser = ClientUser.builder()
                .internalUserId(internalUserId)
                .externalUserId(externalUserId)
                .legalEntityId(legalEntityId)
                .build();

            stubEmptyClientList(internalUserId);
            when(clientApi.createClient(any())).thenReturn(Mono.just(createdClient(clientUuid, internalUserId)));

            StepVerifier.create(service.upsertClients(List.of(clientUser)))
                .expectNextMatches(results -> results.size() == 1
                    && clientUuid.equals(results.getFirst().getInvestmentClientId())
                    && internalUserId.equals(results.getFirst().getInternalUserId())
                    && externalUserId.equals(results.getFirst().getExternalUserId())
                    && legalEntityId.equals(results.getFirst().getLegalEntityId()))
                .verifyComplete();

            verify(clientApi).createClient(any());
            verify(clientApi, never()).patchClient(any(), any());
        }

        @Test
        @DisplayName("duplicate internalUserId — deduplicates and processes once")
        void duplicateInternalUserId_deduplicatesClients() {
            String internalUserId = "duplicate-id";
            UUID clientUuid = UUID.randomUUID();

            ClientUser first = ClientUser.builder()
                .internalUserId(internalUserId)
                .externalUserId("external-a")
                .legalEntityId("le-a")
                .build();
            ClientUser duplicate = ClientUser.builder()
                .internalUserId(internalUserId)
                .externalUserId("external-b")
                .legalEntityId("le-b")
                .build();

            stubEmptyClientList(internalUserId);
            when(clientApi.createClient(any())).thenReturn(Mono.just(createdClient(clientUuid, internalUserId)));

            StepVerifier.create(service.upsertClients(List.of(first, duplicate)))
                .expectNextMatches(results -> results.size() == 1)
                .verifyComplete();

            verify(clientApi, times(1)).listClients(any(), any(), any(), any(), any(),
                eq(internalUserId), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("client upsert fails — skips client and returns empty list")
        void clientUpsertFails_skipsClient() {
            ClientUser clientUser = ClientUser.builder()
                .internalUserId("failing-id")
                .externalUserId("external-fail")
                .legalEntityId("le-fail")
                .build();

            stubEmptyClientList("failing-id");
            when(clientApi.createClient(any()))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            StepVerifier.create(service.upsertClients(List.of(clientUser)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getClient")
    class GetClientTests {

        @Test
        @DisplayName("client found — returns client")
        void clientFound_returnsClient() {
            UUID uuid = UUID.randomUUID();
            OASClient client = new OASClient();
            when(clientApi.getClient(eq(uuid), anyList(), isNull(), isNull())).thenReturn(Mono.just(client));

            StepVerifier.create(service.getClient(uuid))
                .expectNext(client)
                .verifyComplete();
        }

        @Test
        @DisplayName("client not found — returns empty Mono")
        void clientNotFound_returnsEmpty() {
            UUID uuid = UUID.randomUUID();
            WebClientResponseException notFound = new WebClientResponseException(
                404, "Not Found", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
            when(clientApi.getClient(eq(uuid), anyList(), isNull(), isNull())).thenReturn(Mono.error(notFound));

            StepVerifier.create(service.getClient(uuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("non-404 error — propagates error")
        void nonNotFoundError_propagatesError() {
            UUID uuid = UUID.randomUUID();
            when(clientApi.getClient(eq(uuid), anyList(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("service unavailable")));

            StepVerifier.create(service.getClient(uuid))
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && "service unavailable".equals(e.getMessage()))
                .verify();
        }
    }

    @Nested
    @DisplayName("patchClient")
    class PatchClientTests {

        @Test
        @DisplayName("patch succeeds — returns updated client")
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
        @DisplayName("patchClient propagates WebClientResponseException errors")
        void patchClient_webClientError_propagatesError() {
            UUID uuid = UUID.randomUUID();
            PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest();
            when(clientApi.patchClient(eq(uuid), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.BAD_REQUEST.value(), "Bad Request",
                    HttpHeaders.EMPTY, "invalid patch".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8)));

            StepVerifier.create(service.patchClient(uuid, patch))
                .expectError(WebClientResponseException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("updateClient")
    class UpdateClientTests {

        @Test
        @DisplayName("update succeeds — returns updated client")
        void updateClient_success() {
            UUID uuid = UUID.randomUUID();
            OASClientUpdateRequest update = new OASClientUpdateRequest();
            OASClient updated = new OASClient();
            when(clientApi.updateClient(eq(uuid), any())).thenReturn(Mono.just(updated));

            StepVerifier.create(service.updateClient(uuid, update))
                .expectNext(updated)
                .verifyComplete();
        }

        @Test
        @DisplayName("update propagates WebClientResponseException errors")
        void updateClient_webClientError_propagatesError() {
            UUID uuid = UUID.randomUUID();
            OASClientUpdateRequest update = new OASClientUpdateRequest();
            when(clientApi.updateClient(eq(uuid), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.BAD_REQUEST.value(), "Bad Request",
                    HttpHeaders.EMPTY, "invalid update".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8)));

            StepVerifier.create(service.updateClient(uuid, update))
                .expectError(WebClientResponseException.class)
                .verify();
        }
    }

    private void stubEmptyClientList(String internalUserId) {
        lenient().when(clientApi.listClients(any(), any(), any(), any(), any(),
            eq(internalUserId), any(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> Mono.just(new PaginatedOASClientList().results(List.of())));
    }

    private ClientCreate createdClient(UUID clientUuid, String internalUserId) {
        return new ClientCreate(clientUuid).internalUserId(internalUserId);
    }
}
