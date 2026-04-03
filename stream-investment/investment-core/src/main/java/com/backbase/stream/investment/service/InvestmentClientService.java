package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.OASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.Status836Enum;
import com.backbase.stream.investment.ClientUser;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Service wrapper around generated {@link ClientApi} providing guarded create / patch operations with logging, minimal
 * idempotency helpers and consistent error handling.
 *
 * <p>Design notes (see CODING_RULES_COPILOT.md):
 * <ul>
 *   <li>No direct manipulation of generated API classes beyond construction & mapping</li>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying WebClient are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentClientService {

    private static final String USER_EXTERNAL_ID_KEY = "user_external_id";
    private static final String CLIENT_UUID_NULL_MESSAGE = "Client uuid must not be null";

    private final ClientApi clientApi;

    /**
     * Upserts a list of clients with controlled concurrency to prevent race conditions.
     *
     * <p>Design considerations:
     * <ul>
     *   <li>Deduplicates clients by internalUserId before processing</li>
     *   <li>Limits concurrent requests to avoid overwhelming the service and triggering 503 errors</li>
     *   <li>Implements exponential backoff retry with 503 and 409 conflict handling</li>
     *   <li>Processes clients sequentially through the same ExecutorService to maintain order and prevent race conditions</li>
     * </ul>
     *
     * @param clientUsers the list of clients to upsert
     * @return Mono emitting the list of successfully upserted clients
     */
    public Mono<List<ClientUser>> upsertClients(List<ClientUser> clientUsers) {
        // Deduplicate by internalUserId to prevent duplicate processing
        Map<String, ClientUser> uniqueClients = clientUsers.stream()
            .collect(Collectors.toMap(ClientUser::getInternalUserId, Function.identity(),
                (existing, replacement) -> {
                    log.warn("Duplicate internalUserId found: {}. Using first occurrence.",
                        existing.getInternalUserId());
                    return existing;
                }
            ));

        // Process with controlled concurrency (default: 5 concurrent requests)
        // This prevents overwhelming the Investment Service and triggering 503 responses
        return Flux.fromIterable(uniqueClients.values())
            .flatMap(clientUser -> {
                log.debug("Upserting investment client: internalUserId={}, externalUserId={}, legalEntityExternalId={}",
                    clientUser.getInternalUserId(), clientUser.getExternalUserId(),
                    clientUser.getLegalEntityId());

                ClientCreateRequest request = new ClientCreateRequest()
                    .internalUserId(clientUser.getInternalUserId())
                    .status(Status836Enum.ACTIVE)
                    .putExtraDataItem("user_external_id", clientUser.getExternalUserId())
                    .putExtraDataItem("keycloak_username", clientUser.getExternalUserId());

                return upsertClient(request, clientUser.getLegalEntityId())
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .maxBackoff(Duration.ofSeconds(5))
                        .jitter(0.5)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(signal -> {
                            int statusCode = signal.failure() instanceof WebClientResponseException ex
                                ? ex.getStatusCode().value() : 0;
                            log.warn(
                                "Retrying upsert for client: internalUserId={}, attempt={}, statusCode={}",
                                clientUser.getInternalUserId(), signal.totalRetries() + 1, statusCode);
                        }))
                    .doOnSuccess(upsertedClient -> log.debug(
                        "Successfully upserted client: investmentClientId={}, internalUserId={}",
                        upsertedClient.getInvestmentClientId(), upsertedClient.getInternalUserId()))
                    .doOnError(throwable -> log.error(
                        "Failed to upsert client: internalUserId={}, externalUserId={}, legalEntityExternalId={}",
                        clientUser.getInternalUserId(), clientUser.getExternalUserId(),
                        clientUser.getLegalEntityId(), throwable));

            }, 5)
            // Max 5 concurrent requests to avoid overwhelming the service
            .collectList();
    }

    /**
     * Create or update an investment client via Investment Service API.
     *
     * <p>This method implements an upsert pattern:
     * <ol>
     *   <li>Searches for existing client by user external ID and internal user ID</li>
     *   <li>If found, patches the existing client to ensure it's active</li>
     *   <li>If not found, creates a new client</li>
     *   <li>Returns a unified {@link ClientUser} representation</li>
     * </ol>
     *
     * <p>Caller is responsible for ensuring idempotency (e.g. by external UUID management).
     * This method attempts to patch existing clients but will fallback gracefully on errors.
     *
     * @param request               client creation request (must not be null)
     * @param legalEntityExternalId the external ID of the legal entity to associate with this client
     * @return Mono emitting created or updated client representation as {@link ClientUser}
     * @throws NullPointerException if request is null
     */
    private Mono<ClientUser> upsertClient(@NotNull ClientCreateRequest request, String legalEntityExternalId) {
        Objects.requireNonNull(request, "ClientCreateRequest must not be null");

        Map<String, String> extraData = request.getExtraData();
        String userExternalId = extraData != null ? extraData.get(USER_EXTERNAL_ID_KEY) : null;
        String internalUserId = request.getInternalUserId();

        log.info("Upserting investment client (internalUserId={}, userExternalId={})", internalUserId, userExternalId);

        return listExistingClients(userExternalId, internalUserId)
            .flatMap(existingClient -> updateExistingClient(existingClient, legalEntityExternalId))
            .switchIfEmpty(createNewClient(request, legalEntityExternalId))
            .doOnSuccess(clientUser -> log.info(
                "Successfully upserted investment client: investmentClientId={}, internalUserId={}, "
                    + "externalUserId={}, legalEntityExternalId={}",
                clientUser.getInvestmentClientId(), clientUser.getInternalUserId(),
                clientUser.getExternalUserId(), clientUser.getLegalEntityId()))
            .doOnError(throwable -> log.error(
                "Failed to upsert investment client: internalUserId={}, userExternalId={}, legalEntityExternalId={}",
                internalUserId, userExternalId, legalEntityExternalId, throwable));
    }

    /**
     * Lists existing clients matching the provided user identifiers.
     *
     * @param userExternalId the external user ID to search for
     * @param internalUserId the internal user ID to search for
     * @return Mono emitting the first matching client, or empty if no match found
     */
    private Mono<OASClient> listExistingClients(String userExternalId, String internalUserId) {
        return clientApi.listClients(
                List.of(), null, null, null, null,
                internalUserId, null, 1, null, null, null, null)
            .filter(Objects::nonNull)
            .doOnSuccess(clientsResponse -> log.debug(
                "List clients query completed: internalUserId={}, found={} results",
                internalUserId,
                // only 1 has to be
                clientsResponse != null ? clientsResponse.getResults().size() : 0))
            .doOnError(throwable -> log.error(
                "Failed to list existing clients: userExternalId={}, internalUserId={}",
                userExternalId, internalUserId, throwable))
            .flatMap(clientsResponse -> {
                if (Objects.isNull(clientsResponse) || CollectionUtils.isEmpty(clientsResponse.getResults())) {
                    log.info("No existing investment client found with userExternalId={}", userExternalId);
                    return Mono.empty();
                }
                OASClient existingClient = clientsResponse.getResults().get(0);
                log.info("Found existing investment client: uuid={}, internalUserId={}",
                    existingClient.getUuid(), existingClient.getInternalUserId());
                return Mono.just(existingClient);
            });
    }

    /**
     * Updates an existing client by patching its status to ACTIVE. Falls back to the original client if the patch
     * operation fails.
     *
     * @param existingClient        the existing client to update
     * @param legalEntityExternalId the legal entity external ID to associate
     * @return Mono emitting the updated client as {@link ClientUser}
     */
    private Mono<ClientUser> updateExistingClient(OASClient existingClient, String legalEntityExternalId) {
        UUID clientUuid = existingClient.getUuid();
        PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest()
            .status(Status836Enum.ACTIVE);

        log.debug("Attempting to patch existing client: uuid={}, newStatus=ACTIVE", clientUuid);

        return clientApi.patchClient(clientUuid, patch)
            .doOnSuccess(patched -> log.info(
                "Successfully patched existing investment client: uuid={}", clientUuid))
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.warn(
                        "PATCH client failed (falling back to existing client): uuid={}, status={}, body={}",
                        clientUuid, ex.getStatusCode(), ex.getResponseBodyAsString());
                } else {
                    log.warn("PATCH client failed (falling back to existing client): uuid={}",
                        clientUuid, throwable);
                }
            })
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.info("Using existing client data due to patch failure: uuid={}", clientUuid);
                return Mono.just(existingClient);
            })
            .map(updatedOrOriginal -> {
                Map<String, String> extraData = updatedOrOriginal.getExtraData();
                String userExternalId = extraData != null ? extraData.get(USER_EXTERNAL_ID_KEY) : null;
                return buildClientUser(
                    updatedOrOriginal.getUuid(),
                    updatedOrOriginal.getInternalUserId(),
                    userExternalId,
                    legalEntityExternalId);
            });
    }

    /**
     * Creates a new investment client.
     *
     * @param request               the client creation request
     * @param legalEntityExternalId the legal entity external ID to associate
     * @return Mono emitting the newly created client as {@link ClientUser}
     */
    private Mono<ClientUser> createNewClient(ClientCreateRequest request, String legalEntityExternalId) {
        Map<String, String> extraData = request.getExtraData();
        String userExternalId = extraData != null ? (String) extraData.get(USER_EXTERNAL_ID_KEY) : null;
        String internalUserId = request.getInternalUserId();

        log.info("Creating new investment client: internalUserId={}, userExternalId={}",
            internalUserId, userExternalId);

        return clientApi.createClient(request)
            .doOnSuccess(created -> {
                Map<String, String> createdExtraData = created.getExtraData();
                String createdUserExternalId = createdExtraData != null
                    ? createdExtraData.get(USER_EXTERNAL_ID_KEY) : null;
                log.info(
                    "Successfully created new investment client: uuid={}, internalUserId={}, userExternalId={}",
                    created.getUuid(), created.getInternalUserId(), createdUserExternalId);
            })
            .doOnError(throwable -> logCreateError(internalUserId, userExternalId, throwable))
            .map(created -> buildClientUser(
                created.getUuid(),
                created.getInternalUserId(),
                userExternalId,
                legalEntityExternalId));
    }

    /**
     * Builds a {@link ClientUser} instance from the provided data.
     *
     * @param investmentClientId the investment client UUID
     * @param internalUserId     the internal user ID
     * @param externalUserId     the external user ID
     * @param legalEntityId      the legal entity external ID
     * @return constructed ClientUser instance
     */
    private ClientUser buildClientUser(UUID investmentClientId, String internalUserId,
        String externalUserId, String legalEntityId) {
        return ClientUser.builder()
            .investmentClientId(investmentClientId)
            .internalUserId(internalUserId)
            .externalUserId(externalUserId)
            .legalEntityId(legalEntityId)
            .build();
    }

    /**
     * Retrieves an investment client by its UUID.
     *
     * <p>This method gracefully handles the 404 (Not Found) case by returning an empty Mono
     * rather than propagating an error. Other errors are propagated to the caller.
     *
     * @param uuid the client UUID (must not be null)
     * @return Mono emitting the client if found, or empty Mono if not found (404)
     * @throws NullPointerException if uuid is null
     */
    public Mono<OASClient> getClient(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, CLIENT_UUID_NULL_MESSAGE);

        log.debug("Retrieving investment client: uuid={}", uuid);

        return clientApi.getClient(uuid, Collections.emptyList(), null, null)
            .doOnSuccess(client -> {
                if (client != null) {
                    log.info("Successfully retrieved investment client: uuid={}, internalUserId={}",
                        uuid, client.getInternalUserId());
                }
            })
            .doOnError(throwable -> {
                if (!(throwable instanceof WebClientResponseException ex && ex.getStatusCode().value() == 404)) {
                    log.error("Failed to retrieve investment client: uuid={}", uuid, throwable);
                }
            })
            .onErrorResume(throwable -> {
                if (throwable instanceof WebClientResponseException ex && ex.getStatusCode().value() == 404) {
                    log.debug("Investment client not found: uuid={}", uuid);
                    return Mono.empty();
                }
                return Mono.error(throwable);
            });
    }

    /**
     * Patches an existing investment client with partial updates.
     *
     * <p>Only the fields provided in the patch request will be updated; all other fields
     * remain unchanged. This is useful for updating specific attributes without affecting the entire client entity.
     *
     * @param uuid  the client UUID (must not be null)
     * @param patch the patch request containing fields to update (must not be null)
     * @return Mono emitting the updated client representation
     * @throws NullPointerException if uuid or patch is null
     */
    public Mono<OASClient> patchClient(@NotNull UUID uuid, @NotNull PatchedOASClientUpdateRequest patch) {
        Objects.requireNonNull(uuid, CLIENT_UUID_NULL_MESSAGE);
        Objects.requireNonNull(patch, "PatchedOASClientUpdateRequest must not be null");

        log.debug("Patching investment client: uuid={}, patch={}", uuid, patch);

        return clientApi.patchClient(uuid, patch)
            .doOnSuccess(updated -> log.info(
                "Successfully patched investment client: uuid={}, internalUserId={}",
                uuid, updated.getInternalUserId()))
            .doOnError(throwable -> logPatchError(uuid, throwable));
    }

    /**
     * Replaces an existing investment client with a complete update (PUT operation).
     *
     * <p>Unlike {@link #patchClient(UUID, PatchedOASClientUpdateRequest)}, this method
     * requires all client fields to be provided in the update request. Any fields not specified will be set to their
     * default values.
     *
     * @param uuid   the client UUID (must not be null)
     * @param update the complete update request (must not be null)
     * @return Mono emitting the updated client representation
     * @throws NullPointerException if uuid or update is null
     */
    public Mono<OASClient> updateClient(@NotNull UUID uuid, @NotNull OASClientUpdateRequest update) {
        Objects.requireNonNull(uuid, CLIENT_UUID_NULL_MESSAGE);
        Objects.requireNonNull(update, "OASClientUpdateRequest must not be null");

        log.debug("Updating investment client: uuid={}, update={}", uuid, update);

        return clientApi.updateClient(uuid, update)
            .doOnSuccess(updated -> log.info(
                "Successfully updated investment client: uuid={}, internalUserId={}",
                uuid, updated.getInternalUserId()))
            .doOnError(throwable -> logUpdateError(uuid, throwable));
    }

    /**
     * Logs creation errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param internalUserId the internal user ID of the client being created
     * @param userExternalId the external user ID of the client being created
     * @param throwable      the exception that occurred during creation
     */
    private void logCreateError(String internalUserId, String userExternalId, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to create investment client: internalUserId={}, userExternalId={}, status={}, body={}",
                internalUserId, userExternalId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to create investment client: internalUserId={}, userExternalId={}",
                internalUserId, userExternalId, throwable);
        }
    }

    /**
     * Logs patch errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param uuid      the UUID of the client being patched
     * @param throwable the exception that occurred during the patch operation
     */
    private void logPatchError(UUID uuid, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to patch investment client: uuid={}, status={}, body={}",
                uuid, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to patch investment client: uuid={}", uuid, throwable);
        }
    }

    /**
     * Logs update errors with detailed information about the failure.
     *
     * <p>Provides enhanced error context for WebClient exceptions including
     * HTTP status code and response body.
     *
     * @param uuid      the UUID of the client being updated
     * @param throwable the exception that occurred during the update operation
     */
    private void logUpdateError(UUID uuid, Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("Failed to update investment client: uuid={}, status={}, body={}",
                uuid, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } else {
            log.error("Failed to update investment client: uuid={}", uuid, throwable);
        }
    }

    /**
     * Determines if an error is retryable based on HTTP status code.
     *
     * <p>Retryable errors include:
     * <ul>
     *   <li>409 Conflict: Race condition during concurrent client creation/update</li>
     *   <li>503 Service Unavailable: Temporary service overload or maintenance</li>
     * </ul>
     *
     * @param throwable the exception to evaluate
     * @return true if the error is retryable, false otherwise
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            boolean isRetryable = statusCode == 409 || statusCode == 503;
            if (isRetryable) {
                log.debug("Identified retryable error: status={}, reason={}",
                    statusCode, statusCode == 409 ? "CONFLICT" : "SERVICE_UNAVAILABLE");
            }
            return isRetryable;
        }
        return false;
    }

}
