package com.backbase.stream;

import static com.backbase.stream.product.utils.StreamUtils.nullableCollectionToStream;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.service.CustomerProfileService;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
class HelperProcessor {

    protected static final String LEGAL_ENTITY = "LEGAL_ENTITY";
    protected static final String PROCESS_CUSTOMER_PROFILE = "process-customer-profile";
    protected static final String PARTY = "party";
    protected static final String FAILED = "failed";

    protected static final String SERVICE_AGREEMENT = "SERVICE_AGREEMENT";
    protected static final String BUSINESS_FUNCTION_GROUP = "BUSINESS_FUNCTION_GROUP";
    protected static final String USER = "USER";
    protected static final String DEFAULT_DATA_GROUP = "Default data group";
    protected static final String DEFAULT_DATA_DESCRIPTION = "Default data group description";
    protected static final String UPSERT_LEGAL_ENTITY = "upsert-legal-entity";
    protected static final String EXISTS = "exists";
    protected static final String CREATED = "created";

    protected static final String UPDATED = "updated";
    protected static final String PROCESS_PRODUCTS = "process-products";
    protected static final String PROCESS_JOB_PROFILES = "process-job-profiles";
    protected static final String PROCESS_LIMITS = "process-limits";
    protected static final String PROCESS_CONTACTS = "process-contacts";
    protected static final String REJECTED = "rejected";
    protected static final String UPSERT = "upsert";
    protected static final String SETUP_SERVICE_AGREEMENT = "setup-service-agreement";
    protected static final String BATCH_PRODUCT_GROUP_ID = "batch_product_group_task-";

    protected static final String LEGAL_ENTITY_E_TYPE = "LE";
    protected static final String SERVICE_AGREEMENT_E_TYPE = "SA";
    protected static final String FUNCTION_GROUP_E_TYPE = "FAG";
    protected static final String FUNCTION_E_TYPE = "FUN";
    protected static final String PRIVILEGE_E_TYPE = "PRV";
    protected static final String JOB_ROLE_LIMITS = "job-role-limits";
    protected static final String USER_JOB_ROLE_LIMITS = "user-job-role-limits";
    protected static final String LEGAL_ENTITY_LIMITS = "legal-entity-limits";
    protected static final String IDENTITY_USER = "IDENTITY_USER";

    private <T extends StreamTask> boolean isValidParty(
        T legalEntityTask,
        List<Party> parties,
        String legalEntityInternalId,
        String legalEntityExternalId) {
        if (isEmpty(parties)) {
            legalEntityTask.info(LEGAL_ENTITY, PROCESS_CUSTOMER_PROFILE, "skipped", legalEntityExternalId,
                legalEntityInternalId, "No parties found in Legal Entity to process.");
            return false;
        }
        return true;
    }


    public <T extends StreamTask> Mono<T> processParties(
        T legalEntityTask,
        List<Party> parties,
        String legalEntityInternalId,
        String legalEntityExternalId,
        CustomerProfileService customerProfileService
    ) {
        log.info("Processing Customer Profile Parties for LE: {}", legalEntityExternalId);

        if (!isValidParty(legalEntityTask, parties, legalEntityInternalId, legalEntityExternalId)) {
            return Mono.just(legalEntityTask);
        }

        var processingErrors = new CopyOnWriteArrayList<Throwable>();

        return Flux.fromStream(nullableCollectionToStream(parties))
            .filter(Objects::nonNull)
            .concatMap(party -> handlePartyUpsert(
                party,
                legalEntityInternalId,
                legalEntityExternalId,
                customerProfileService,
                processingErrors,
                legalEntityTask
            ))
            .then(Mono.fromRunnable(() -> logFinalProcessingStatus(
                processingErrors,
                legalEntityInternalId,
                legalEntityExternalId,
                legalEntityTask
            )))
            .thenReturn(legalEntityTask);
    }

    private <T extends StreamTask> void logFinalProcessingStatus(
        List<Throwable> processingErrors,
        String legalEntityInternalId,
        String legalEntityExternalId,
        T legalEntityTask
    ) {
        if (!processingErrors.isEmpty()) {
            int errorCount = processingErrors.size();
            log.warn("Completed processing parties for LE {} with {} error(s).", legalEntityExternalId, errorCount);
            legalEntityTask.warn(LEGAL_ENTITY, PROCESS_CUSTOMER_PROFILE, "completed_with_errors",
                legalEntityExternalId, legalEntityInternalId,
                "Party processing completed with %d error(s).", errorCount);
        } else {
            log.info("Successfully processed all parties for LE {}.", legalEntityExternalId);
            legalEntityTask.info(LEGAL_ENTITY, PROCESS_CUSTOMER_PROFILE, "completed_successfully",
                legalEntityExternalId, legalEntityInternalId,
                "Party processing completed successfully.");
        }
    }

    private <T extends StreamTask> Mono<Void> handlePartyUpsert(
        Party party,
        String legalEntityInternalId,
        String legalEntityExternalId,
        CustomerProfileService customerProfileService,
        List<Throwable> processingErrors,
        T legalEntityTask
    ) {
        String partyId = party.getPartyId();
        log.debug("Attempting to upsert party with partyId: {}", partyId);

        return customerProfileService.upsertParty(party, legalEntityInternalId)
            .doOnSuccess(result ->
                legalEntityTask.info(PARTY, PROCESS_CUSTOMER_PROFILE, "upserted", partyId, null,
                    "Successfully upserted party: %s for LE: %s", partyId, legalEntityExternalId)
            )
            .doOnError(throwable -> {
                log.error("Failed to upsert party {}: {}", partyId, throwable.getMessage(), throwable);
                processingErrors.add(throwable);
                legalEntityTask.error(PARTY, PROCESS_CUSTOMER_PROFILE, FAILED, partyId, null, throwable,
                    throwable.getMessage(), "Error upserting party: %s for LE: %s", partyId, legalEntityExternalId);
            })
            .onErrorResume(throwable -> Mono.empty())
            .then();
    }
}
