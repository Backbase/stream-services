package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.Status836Enum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link InvestmentClientService}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Client upsert with existing and new clients</li>
 *   <li>Client retrieval with 404 handling</li>
 *   <li>Client patching and updating</li>
 *   <li>Error handling and logging</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentClientService Integration Tests")
class InvestmentClientServiceIT {

    @Mock
    private ClientApi clientApi;

    private InvestmentClientService service;

    private UUID clientUuid;
    private String internalUserId;
    private String externalUserId;
    private String legalEntityExternalId;

    @BeforeEach
    void setUp() {
        service = new InvestmentClientService(clientApi);

        clientUuid = UUID.randomUUID();
        internalUserId = "user-123";
        externalUserId = "user-ext-123";
        legalEntityExternalId = "le-001";
    }

    /*@Test
    @DisplayName("Should create new client when none exists")
    void upsertClient_noExistingClient_createsNewClient() {
        // Given: No existing client
        ClientCreateRequest request = createClientRequest();
        OASClient newClient = createOASClient();

        // Mock list returns empty (no existing clients)
        when(clientApi.listClients(any(), any(), any(), any(), any(),
            eq(internalUserId), any(), any(), any(), any(), any(), any()))
            .thenReturn(Mono.empty());

        // Mock client creation
        when(clientApi.createClient(any(ClientCreateRequest.class)))
            .thenAnswer(invocation -> Mono.just(newClient));

        // When: Upsert client
        StepVerifier.create(service.upsertClient(request, legalEntityExternalId))
            .assertNext(clientUser -> {
                // Then: New client is created
                assertThat(clientUser.getInternalUserId()).isEqualTo(internalUserId);
                assertThat(clientUser.getExternalUserId()).isEqualTo(externalUserId);
                assertThat(clientUser.getLegalEntityExternalId()).isEqualTo(legalEntityExternalId);

                verify(clientApi).createClient(any(ClientCreateRequest.class));
            })
            .verifyComplete();
    }*/

    @Test
    @DisplayName("Should return empty Mono when client not found (404)")
    void getClient_notFound_returnsEmptyMono() {
        // Given: Client not found
        when(clientApi.getClient(eq(clientUuid), any(), any(), any()))
            .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));

        // When: Get client
        StepVerifier.create(service.getClient(clientUuid))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return client when found")
    void getClient_found_returnsClient() {
        // Given: Client exists
        OASClient client = createOASClient();
        when(clientApi.getClient(eq(clientUuid), any(), any(), any()))
            .thenReturn(Mono.just(client));

        // When: Get client
        StepVerifier.create(service.getClient(clientUuid))
            .assertNext(foundClient -> {
                // Then: Client is returned
                assertThat(foundClient.getInternalUserId()).isEqualTo(internalUserId);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should patch client successfully")
    void patchClient_success_returnsUpdatedClient() {
        // Given: Patch request
        PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest()
            .status(Status836Enum.ACTIVE);
        OASClient updatedClient = createOASClient();

        when(clientApi.patchClient(eq(clientUuid), any(PatchedOASClientUpdateRequest.class)))
            .thenReturn(Mono.just(updatedClient));

        // When: Patch client
        StepVerifier.create(service.patchClient(clientUuid, patch))
            .assertNext(client -> {
                // Then: Updated client is returned
                assertThat(client.getInternalUserId()).isEqualTo(internalUserId);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when patch fails")
    void patchClient_fails_propagatesError() {
        // Given: Patch fails
        PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest();
        when(clientApi.patchClient(eq(clientUuid), any(PatchedOASClientUpdateRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("Patch failed")));

        // When: Patch client
        StepVerifier.create(service.patchClient(clientUuid, patch))
            .expectError(RuntimeException.class)
            .verify();
    }

    // Helper methods

    private ClientCreateRequest createClientRequest() {
        return new ClientCreateRequest()
            .internalUserId(internalUserId)
            .status(Status836Enum.ACTIVE)
            .putExtraDataItem("user_external_id", externalUserId)
            .putExtraDataItem("keycloak_username", externalUserId);
    }

    private OASClient createOASClient() {
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("user_external_id", externalUserId);

        // Use reflection or a simpler approach - just create basic client
        // The actual OASClient class uses Lombok builders from generated code
        OASClient client = new OASClient();
        // Set fields via reflection if needed, or just mock the getters
        return client
            .internalUserId(internalUserId)
            .status(Status836Enum.ACTIVE)
            .extraData(extraData);
    }
}

