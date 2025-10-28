package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.OASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.Status836Enum;
import com.backbase.stream.investment.ClientUser;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Service wrapper around generated {@link ClientApi} providing guarded create / patch operations with logging, minimal
 * idempotency helpers and consistent error handling. Design notes (see CODING_RULES_COPILOT.md): - No direct
 * manipulation of generated API classes beyond construction & mapping. - Side-effecting operations are logged at info
 * (create) or debug (patch) levels. - Exceptions from the underlying WebClient are propagated (caller decides retry
 * strategy).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentClientService {

    private final ClientApi clientApi;

    /**
     * Create a new investment client via Investment Service API. Caller is responsible for ensuring idempotency (e.g.
     * by external UUID management) – this method does not attempt an existence check to avoid an extra round trip
     * unless requested.
     *
     * @param request               request body (must not be null)
     * @param legalEntityExternalId
     * @return Mono emitting created client representation
     */
    public Mono<ClientUser> upsertClient(@NotNull ClientCreateRequest request, String legalEntityExternalId) {
        Objects.requireNonNull(request, "ClientCreateRequest must not be null");
        log.info("Creating investment client (internalUserId={})", safeInternalUserId(request));

        return clientApi.listClients(List.of(), null,
                Map.of("extra_data__user_external_id", request.getExtraData().get("user_external_id")), null, null,
                request.getInternalUserId(),
                null, null, null, null, null, List.of())
            .flatMap(clients -> {
                if (Objects.isNull(clients) || CollectionUtils.isEmpty(clients.getResults())) {
                    log.info("No existing investment client with externalUserId={}, proceeding with creation",
                        request.getExtraData().get("user_external_id"));
                    return Mono.empty();
                }
                OASClient client = clients.getResults().get(0);
                // Attempt minimal PATCH (only status) to avoid validation 400; fallback to existing client if PATCH fails.
                PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest().status(Status836Enum.ACTIVE);
                log.debug("Attempting minimal PATCH client uuid={} payload={} ", client.getUuid(), patch);
                return clientApi.patchClient(client.getUuid(), patch)
                    .doOnSuccess(updated -> log.info("Patched existing investment client uuid={} internalUserId={} status={}",
                        client.getUuid(), client.getInternalUserId(), updated != null ? updated.getStatus() : null))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("PATCH client uuid={} failed: status={} body={}, falling back to existing client", client.getUuid(), ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(client); // fallback original
                    })
                    .map(updatedOrOriginal -> ClientUser.builder()
                        .investmentClientId(updatedOrOriginal.getUuid())
                        .externalUserId((String) updatedOrOriginal.getExtraData().get("user_external_id"))
                        .internalUserId(updatedOrOriginal.getInternalUserId())
                        .build());
            })
            .switchIfEmpty(clientApi.createClient(request)
                .flatMap(created -> Mono.just(ClientUser.builder()
                    .investmentClientId(created.getUuid())
                    .externalUserId((String) request.getExtraData().get("user_external_id"))
                    .internalUserId(safeInternalUserId(request))
                    .build())))
            .doOnSuccess(response -> {
                response.setLegalEntityExternalId(legalEntityExternalId);
                log.debug("List clients response: body={}", response);
            })
            .doOnError(throwable -> log.error("List clients failed", throwable));
    }

    /**
     * Retrieve a client, emitting empty Mono if not found (404).
     */
    public Mono<OASClient> getClient(UUID uuid) {
        Objects.requireNonNull(uuid, "Client uuid must not be null");
        return clientApi.getClient(uuid, Collections.emptyList(), null, null)
            .onErrorResume(throwable -> {
                if (throwable instanceof WebClientResponseException ex && ex.getStatusCode().value() == 404) {
                    log.debug("Client uuid={} not found (404)", uuid);
                    return Mono.empty();
                }
                return Mono.error(throwable);
            });
    }

    /**
     * Patch an existing client. Only provided fields in the patch request are updated.
     *
     * @param uuid  Client UUID (required)
     * @param patch patch body (can be null -> returns Mono.error)
     * @return Mono with updated client view
     */
    public Mono<OASClient> patchClient(@NotNull UUID uuid, @NotNull PatchedOASClientUpdateRequest patch) {
        Objects.requireNonNull(uuid, "Client uuid must not be null");
        Objects.requireNonNull(patch, "PatchedOASClientUpdateRequest must not be null");
        log.debug("Patching investment client uuid={}", uuid);
        return clientApi.patchClient(uuid, patch)
            .doOnSuccess(updated -> log.info("Patched investment client uuid={}", uuid))
            .doOnError(throwable -> logPatchError(uuid, throwable));
    }

    /**
     * Replace (PUT) an existing client.
     *
     * @param uuid   client uuid
     * @param update full update request
     * @return updated client
     */
    public Mono<OASClient> updateClient(@NotNull UUID uuid, @NotNull OASClientUpdateRequest update) {
        Objects.requireNonNull(uuid, "Client uuid must not be null");
        Objects.requireNonNull(update, "OASClientUpdateRequest must not be null");
        log.debug("Updating investment client uuid={}", uuid);
        return clientApi.updateClient(uuid, update)
            .doOnSuccess(updated -> log.info("Updated investment client uuid={}", uuid))
            .doOnError(throwable -> logUpdateError(uuid, throwable));
    }

    private void logCreateError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to create investment client: status={} body={}", ex.getStatusCode(),
                ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to create investment client", throwable);
        }
    }

    private void logPatchError(UUID uuid, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to patch investment client uuid={}: status={} body={}", uuid, ex.getStatusCode(),
                ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to patch investment client uuid={}", uuid, throwable);
        }
    }

    private void logUpdateError(UUID uuid, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to update investment client uuid={}: status={} body={}", uuid, ex.getStatusCode(),
                ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to update investment client uuid={}", uuid, throwable);
        }
    }

    @Nullable
    private String safeInternalUserId(ClientCreateRequest request) {
        try {
            // Rely on generated accessor naming; defensively catch in case spec changes.
            return (String) request.getClass().getMethod("getInternalUserId").invoke(request);
        } catch (Exception e) {
            return null;
        }
    }
}
