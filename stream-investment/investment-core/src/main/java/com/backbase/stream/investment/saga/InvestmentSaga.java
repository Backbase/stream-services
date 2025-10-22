package com.backbase.stream.investment.saga;

import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.Status836Enum;
import com.backbase.stream.configuration.InvestmentSagaConfigurationProperties;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.StreamTask.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Saga orchestrating investment client ingestion (create, patch, update) using {@link InvestmentClientService}. Focuses
 * on idempotent create (optional) and safe patch/update operations, writing progress to the {@link StreamTask} history
 * for observability.
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentSaga implements StreamTaskExecutor<InvestmentTask> {

    public static final String INVESTMENT = "investment-client";
    public static final String OP_CREATE = "create";
    public static final String OP_PATCH = "patch";
    public static final String OP_UPDATE = "update";
    public static final String RESULT_CREATED = "created";
    public static final String RESULT_SKIPPED = "skipped";
    public static final String RESULT_PATCHED = "patched";
    public static final String RESULT_UPDATED = "updated";
    public static final String RESULT_FAILED = "failed";

    private final InvestmentClientService clientService;
    private final InvestmentSagaConfigurationProperties properties;
    private boolean enabled;

    @Override
    public Mono<InvestmentTask> executeTask(InvestmentTask streamTask) {

        return upsertClients(streamTask);
    }

    public Mono<InvestmentTask> upsertClients(InvestmentTask streamTask) {
        InvestmentData investmentData = streamTask.getData();

        streamTask.info(INVESTMENT, "upsert", null, streamTask.getName(), streamTask.getId(),
            "Process Investment Clients");
        streamTask.setState(State.IN_PROGRESS);
        return Flux.fromIterable(investmentData.getClientUsers())
            .flatMap(clientUser -> clientService.upsertClient(new ClientCreateRequest()
                .internalUserId(clientUser.getInternalUserId())
                .status(Status836Enum.ACTIVE)
                .putExtraDataItem("user_external_id", clientUser.getExternalUserId())
                .putExtraDataItem("keycloak_username", clientUser.getExternalUserId())
            ))
            .collectList()
            .map(clients -> {
                streamTask.data(clients);
                streamTask.info(INVESTMENT, "upsert", RESULT_CREATED, streamTask.getName(), streamTask.getId(),
                    "Upserted " + clients.size() + " Investment Clients");
                streamTask.setState(State.COMPLETED);
                return streamTask;
            });

    }

    @Override
    public Mono<InvestmentTask> rollBack(InvestmentTask streamTask) {
        return null;
    }

    public boolean isEnabled() {
        return true;//enabled;
    }


    /*private Mono<InvestmentClientTask> handleCreate(InvestmentClientTask task) {
        ClientCreateRequest request = task.getCreateRequest();
        if (request == null) {
            return fail(task, OP_CREATE, "ClientCreateRequest is required for CREATE operation");
        }
        // Try resolve uuid field reflectively – spec may include or server may allocate.
        UUID requestedUuid = extractUuid(request);
        Mono<Boolean> existsCheck = properties.isPreExistenceCheck() && requestedUuid != null
            ? clientService.getClient(requestedUuid).hasElement()
            : Mono.just(false);

        return existsCheck.flatMap(exists -> {
            if (exists) {
                task.info(ENTITY, OP_CREATE, RESULT_SKIPPED, safeExternalId(requestedUuid), null,
                    "Client already exists, skipping create");
                task.setState(StreamTask.State.COMPLETED);
                return Mono.just(task);
            }
            return clientService.createClient(request)
                .map(created -> afterCreateSuccess(task, created))
                .onErrorResume(throwable -> failWithException(task, OP_CREATE, throwable, "Failed to create client"));
        });
    }*/

    /*private InvestmentClientTask afterCreateSuccess(InvestmentClientTask task, ClientCreate created) {
        task.setCreatedClient(created);
        task.setClientUuid(created != null ? created.getUuid() : null);
        task.info(ENTITY, OP_CREATE, RESULT_CREATED, safeExternalId(created != null ? created.getUuid() : null), null,
            "Client created");
        task.setState(StreamTask.State.COMPLETED);
        return task;
    }

    private Mono<InvestmentClientTask> handlePatch(InvestmentClientTask task) {
        UUID uuid = task.getClientUuid();
        PatchedOASClientUpdateRequest patch = task.getPatchRequest();
        if (uuid == null || patch == null) {
            return fail(task, OP_PATCH, "clientUuid and patchRequest are required for PATCH operation");
        }
        return clientService.patchClient(uuid, patch)
            .map(updated -> afterPatchSuccess(task, updated))
            .onErrorResume(throwable -> failWithException(task, OP_PATCH, throwable, "Failed to patch client"));
    }

    private InvestmentClientTask afterPatchSuccess(InvestmentClientTask task, OASClient updated) {
        task.setUpdatedClient(updated);
        task.info(ENTITY, OP_PATCH, RESULT_PATCHED, safeExternalId(task.getClientUuid()), null, "Client patched");
        task.setState(StreamTask.State.COMPLETED);
        return task;
    }

    private Mono<InvestmentClientTask> handleUpdate(InvestmentClientTask task) {
        UUID uuid = task.getClientUuid();
        var update = task.getUpdateRequest();
        if (uuid == null || update == null) {
            return fail(task, OP_UPDATE, "clientUuid and updateRequest are required for UPDATE operation");
        }
        return clientService.updateClient(uuid, update)
            .map(updated -> afterUpdateSuccess(task, updated))
            .onErrorResume(throwable -> failWithException(task, OP_UPDATE, throwable, "Failed to update client"));
    }

    private InvestmentClientTask afterUpdateSuccess(InvestmentClientTask task, OASClient updated) {
        task.setUpdatedClient(updated);
        task.info(ENTITY, OP_UPDATE, RESULT_UPDATED, safeExternalId(task.getClientUuid()), null, "Client updated");
        task.setState(StreamTask.State.COMPLETED);
        return task;
    }

    private Mono<InvestmentClientTask> fail(InvestmentClientTask task, String operation, String message) {
        task.error(ENTITY, operation, RESULT_FAILED, safeExternalId(task.getClientUuid()), null, message);
        task.setState(StreamTask.State.FAILED);
        return Mono.just(task);
    }

    private Mono<InvestmentClientTask> failWithException(InvestmentClientTask task, String operation,
        Throwable throwable, String errorMessage) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("{} operation failed: status={} body={}", operation, ex.getStatusCode(),
                ex.getResponseBodyAsString(), ex);
            task.error(ENTITY, operation, RESULT_FAILED, safeExternalId(task.getClientUuid()), null, throwable,
                errorMessage, "{}", ex.getResponseBodyAsString());
        } else {
            log.error("{} operation failed", operation, throwable);
            task.error(ENTITY, operation, RESULT_FAILED, safeExternalId(task.getClientUuid()), null, throwable,
                errorMessage, throwable.getMessage());
        }
        task.setState(StreamTask.State.FAILED);
        return Mono.just(task);
    }*/

}

