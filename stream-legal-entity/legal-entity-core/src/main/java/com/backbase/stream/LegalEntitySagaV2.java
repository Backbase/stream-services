package com.backbase.stream;

import static com.backbase.stream.product.utils.StreamUtils.nullableCollectionToStream;
import static com.backbase.stream.service.UserService.REMOVED_PREFIX;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.backbase.audiences.collector.api.service.v1.model.CustomerOnboardedRequest;
import com.backbase.audiences.collector.api.service.v1.model.CustomerOnboardedRequest.UserKindEnum;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.Entity;
import com.backbase.dbs.limit.api.service.v2.model.PeriodicLimitsBounds;
import com.backbase.dbs.limit.api.service.v2.model.TransactionalLimitsBound;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.GetUsersList;
import com.backbase.dbs.user.profile.api.service.v2.model.CreateUserProfile;
import com.backbase.stream.audiences.UserKindSegmentationSaga;
import com.backbase.stream.audiences.UserKindSegmentationTask;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.exceptions.AccessGroupException;
import com.backbase.stream.exceptions.LegalEntityException;
import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionLimit;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityParticipantV2;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.Limit;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.mapper.ExternalContactMapper;
import com.backbase.stream.mapper.UserProfileMapper;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.BusinessFunctionGroupMapper;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * Legal Entity Saga. This Service creates Legal Entities and their supporting objects from a {@link LegalEntity}
 * aggregate object. For each Legal Entity object it will either retrieve the existing Legal Entity or create a new one.
 * Next it will either create of update the Administrator users which are used to create a Master Service agreement.
 * After the users are created / retrieved and enriched with their internal Ids we can setup the Master Service
 */
@Slf4j
public class LegalEntitySagaV2 implements StreamTaskExecutor<LegalEntityTaskV2> {

    public static final String LEGAL_ENTITY = "LEGAL_ENTITY";
    public static final String IDENTITY_USER = "IDENTITY_USER";
    public static final String SERVICE_AGREEMENT = "SERVICE_AGREEMENT";
    public static final String BUSINESS_FUNCTION_GROUP = "BUSINESS_FUNCTION_GROUP";
    public static final String USER = "USER";
    private static final String DEFAULT_DATA_GROUP = "Default data group";
    private static final String DEFAULT_DATA_DESCRIPTION = "Default data group description";
    public static final String UPSERT_LEGAL_ENTITY = "upsert-legal-entity";
    public static final String FAILED = "failed";
    public static final String EXISTS = "exists";
    public static final String CREATED = "created";

    public static final String UPDATED = "updated";
    public static final String PROCESS_PRODUCTS = "process-products";
    public static final String PROCESS_JOB_PROFILES = "process-job-profiles";
    public static final String PROCESS_LIMITS = "process-limits";
    public static final String PROCESS_CONTACTS = "process-contacts";
    public static final String REJECTED = "rejected";
    public static final String UPSERT = "upsert";
    public static final String SETUP_SERVICE_AGREEMENT = "setup-service-agreement";
    private static final String BATCH_PRODUCT_GROUP_ID = "batch_product_group_task-";

    private static final String LEGAL_ENTITY_E_TYPE = "LE";
    private static final String SERVICE_AGREEMENT_E_TYPE = "SA";
    private static final String FUNCTION_E_TYPE = "FUN";
    private static final String PRIVILEGE_E_TYPE = "PRV";
    private static final String LEGAL_ENTITY_LIMITS = "legal-entity-limits";

    private final BusinessFunctionGroupMapper businessFunctionGroupMapper = Mappers.getMapper(BusinessFunctionGroupMapper.class);
    private final UserProfileMapper userProfileMapper = Mappers.getMapper(UserProfileMapper.class);

    private final LegalEntityService legalEntityService;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final AccessGroupService accessGroupService;
    private final BatchProductIngestionSaga batchProductIngestionSaga;
    private final LimitsSaga limitsSaga;
    private final ContactsSaga contactsSaga;
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties;
    private final UserKindSegmentationSaga userKindSegmentationSaga;

    private static final ExternalContactMapper externalContactMapper = ExternalContactMapper.INSTANCE;

    public LegalEntitySagaV2(LegalEntityService legalEntityService,
        UserService userService,
        UserProfileService userProfileService,
        AccessGroupService accessGroupService,
        BatchProductIngestionSaga batchProductIngestionSaga,
        LimitsSaga limitsSaga,
        ContactsSaga contactsSaga,
        LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties,
        UserKindSegmentationSaga userKindSegmentationSaga) {
        this.legalEntityService = legalEntityService;
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.accessGroupService = accessGroupService;
        this.batchProductIngestionSaga = batchProductIngestionSaga;
        this.limitsSaga = limitsSaga;
        this.contactsSaga = contactsSaga;
        this.legalEntitySagaConfigurationProperties = legalEntitySagaConfigurationProperties;
        this.userKindSegmentationSaga = userKindSegmentationSaga;
    }

    @Override
    public Mono<LegalEntityTaskV2> executeTask(@SpanTag(value = "streamTask") LegalEntityTaskV2 streamTask) {
        return upsertLegalEntity(streamTask)
            .flatMap(this::linkLegalEntityToRealm)
            .flatMap(this::setupAdministrators)
            .flatMap(this::setupUsers)
            .flatMap(this::processAudiencesSegmentation)
            .flatMap(this::setupServiceAgreement)
            .flatMap(this::setupLimits)
            .flatMap(this::postLegalEntityContacts)
            .flatMap(this::processSubsidiaries);
    }

    private Mono<LegalEntityTaskV2> processAudiencesSegmentation(LegalEntityTaskV2 streamTask) {
        if (!userKindSegmentationSaga.isEnabled()) {
            log.info("Skipping audiences UserKind segmentation - feature is disabled.");
            return Mono.just(streamTask);
        }

        var le = streamTask.getData();

        if (le.getLegalEntityType() != LegalEntityType.CUSTOMER) {
            return Mono.just(streamTask);
        }

        var customerCategory = le.getCustomerCategory();
        if (customerCategory == null) {
            var defaultCategory = userKindSegmentationSaga.getDefaultCustomerCategory();
            if (defaultCategory == null) {
                return Mono.error(new StreamTaskException(streamTask,
                    "Failed to determine LE customerCategory for UserKindSegmentationSage."));
            }
            customerCategory = CustomerCategory.fromValue(defaultCategory);
        }

        var userKind = customerCategoryToUserKind(customerCategory);

        if (userKind == null) {
            log.info("Skipping audiences UserKind segmentation - customerCategory " + customerCategory
                + " is not supported");
            return Mono.just(streamTask);
        }

        log.info("Ingesting customers of LE into UserKind segment customerCategory: " + customerCategory);

        return Flux.fromStream(StreamUtils.nullableCollectionToStream(le.getUsers()))
            .map(user -> {
                var task = new UserKindSegmentationTask();
                task.setCustomerOnboardedRequest(
                    new CustomerOnboardedRequest()
                        .internalUserId(user.getInternalId())
                        .userKind(userKind)
                );
                return task;
            })
            .flatMap(userKindSegmentationSaga::executeTask)
            .then(Mono.just(streamTask));
    }

    private UserKindEnum customerCategoryToUserKind(CustomerCategory customerCategory) {
        switch (customerCategory) {
            case RETAIL -> {
                return UserKindEnum.RETAILCUSTOMER;
            }
            case BUSINESS -> {
                return UserKindEnum.SME;
            }
            default -> {
                return null;
            }
        }
    }

    private Mono<LegalEntityTaskV2> postLegalEntityContacts(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        if (isEmpty(legalEntity.getContacts())) {
            streamTask.info(LEGAL_ENTITY, PROCESS_CONTACTS, FAILED, legalEntity.getExternalId(), legalEntity.getInternalId(),
                    "Legal Entity: %s does not have any Contacts defined", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }
        ServiceAgreementV2 serviceAgreement = getServiceAgreement(legalEntity);
        log.info("Creating Contacts for Legal Entity Id {}", legalEntity.getExternalId());
        Optional<String> externalUserOptional = Optional.empty();
        Optional<User> optionalUser = legalEntity.getUsers().stream().findFirst();
        if (optionalUser.isPresent()) {
            externalUserOptional = Optional.of(optionalUser.get().getExternalId());
        }
        if (externalUserOptional.isEmpty()) {
            streamTask.info(LEGAL_ENTITY, PROCESS_CONTACTS, FAILED, legalEntity.getExternalId(), legalEntity.getInternalId(),
                    "Legal Entity: %s does not have any Users", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }
        return contactsSaga.executeTask(createContactsTask(streamTask.getId(), legalEntity.getExternalId(), serviceAgreement.getExternalId(), externalUserOptional.get(), AccessContextScope.LE, legalEntity.getContacts()))
                .flatMap(contactsTask -> requireNonNull(Mono.just(streamTask)))
                .then(Mono.just(streamTask));
    }

    private String getParticipantUser(ServiceAgreement serviceAgreement) {
        if (!isEmpty(serviceAgreement.getParticipants())) {
            Optional<LegalEntityParticipant> participants = serviceAgreement.getParticipants()
                    .stream()
                    .filter(LegalEntityParticipant::getSharingUsers)
                    .findFirst();
            if (participants.isPresent()) {
                LegalEntityParticipant participant = participants.get();
                if (!isEmpty(participant.getAdmins())) {
                    return getOptionalUserId(participant.getAdmins().stream().findFirst());
                } else if (!isEmpty(participant.getUsers())) {
                    return getOptionalUserId(participant.getUsers().stream().findFirst());
                }
            }
        }
        return null;
    }

    private String getOptionalUserId(Optional<String> optionalUserId) {
        return optionalUserId.isPresent() ? optionalUserId.get() : null;
    }

    private ContactsTask createContactsTask(String streamTaskId, String externalLegalEntityId, String externalServiceAgreementId, String externalUserId, AccessContextScope scope, List<ExternalContact> contacts) {
        var contactData = new ContactsBulkPostRequestBody();
        contactData.setIngestMode(IngestMode.UPSERT);
        contactData.setAccessContext(createExternalAccessContext(externalLegalEntityId, externalServiceAgreementId, externalUserId, scope));
        contactData.setContacts(externalContactMapper.toMapList(contacts));
        return new ContactsTask(streamTaskId + "-" + "contacts-task", contactData);
    }

    private ExternalAccessContext createExternalAccessContext(String externalLegalEntityId, String externalServiceAgreementId, String externalUserId, AccessContextScope scope) {
        ExternalAccessContext accessContext = new ExternalAccessContext();
        accessContext.setExternalLegalEntityId(externalLegalEntityId);
        accessContext.setExternalServiceAgreementId(externalServiceAgreementId);
        accessContext.setExternalUserId(externalUserId);
        accessContext.setScope(scope);
        return accessContext;
    }

    private Optional<String> getUserExternalId(List<JobProfileUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Optional.empty();
        }
        Optional<JobProfileUser> optionalUser = users.stream().findFirst();
        return optionalUser.map(jobProfileUser -> Optional.ofNullable(jobProfileUser.getUser().getExternalId())).orElse(Optional.empty());
    }

    private ServiceAgreementV2 getServiceAgreement(LegalEntityV2 legalEntity) {
        return legalEntity.getMasterServiceAgreement();
    }

    @Override
    public Mono<LegalEntityTaskV2> rollBack(LegalEntityTaskV2 streamTask) {
        // GET CREATED AND EVENTS AND CALL DELETE ENDPOINTS IN REVERSE
        return Mono.just(streamTask);
    }

    /**
     * Delete Legal Entity by provided Legal Entity external ID.
     * <i>This call doesn't cover arrangements and transactions removal. Should be done before caling this method.</i>
     * </br>
     * Flow is the following:
     * <ul>
     *     <li>Remove permissions for all Function Groups for all users in LE</li>
     *     <li>Delete all Function Groups for Master Service Agreement</li>
     *     <li>Delete all Administrators</li>
     *     <li>'Archive' all Users in LE</li>
     *     <li>Remove Legal Entity itself.</li>
     * </ul>
     *
     * @param legalEntityExternalId legal entity external ID.
     * @return Mono<Void>
     */
    public Mono<Void> deleteLegalEntity(String legalEntityExternalId, int userQuerySize) {
        AtomicInteger from = new AtomicInteger(0);
        return Mono.zip(
                legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(legalEntityExternalId),
                legalEntityService.getLegalEntityByExternalId(legalEntityExternalId))
            .flatMap(data -> {
                LegalEntity le = data.getT2();
                return userService.getUsersByLegalEntity(le.getInternalId(), userQuerySize, from.get())
                    .expand(response -> {
                        int totalPages = response.getTotalElements().intValue() / userQuerySize;
                        from.getAndIncrement();
                        if (from.get() > totalPages) {
                            return Mono.empty();
                        }
                        return userService.getUsersByLegalEntity(le.getInternalId(), userQuerySize, from.get());
                    })
                    .map(GetUsersList::getUsers)
                    .collectList()
                    .flatMap(lists -> Mono.just(
                        lists.stream().flatMap(List::stream).filter(getUser -> !getUser.getExternalId().startsWith(
                            REMOVED_PREFIX)).collect(Collectors.toList())))
                    .map(getUsers -> Tuples.of(data.getT1(), le, getUsers));
            })
            .flatMap(data -> {
                ServiceAgreement sa = data.getT1();
                List<GetUser> users = data.getT3();
                return Flux.fromIterable(users)
                    .flatMap(user -> accessGroupService.removePermissionsForUser(sa.getInternalId(), user.getId())
                        .thenReturn(user.getExternalId()))
                    .collectList()
                    .map(userIds -> Tuples.of(sa, data.getT2(), userIds));

            })
            .flatMap(data -> {
                ServiceAgreement sa = data.getT1();
                LegalEntity le = data.getT2();
                List<String> userIds = data.getT3();
                return accessGroupService.deleteFunctionGroupsForServiceAgreement(sa.getInternalId())
                    .then(accessGroupService.deleteAdmins(sa))
                    .then(userService.archiveUsers(le.getInternalId(), userIds))
                    .then(legalEntityService.deleteLegalEntity(legalEntityExternalId));
            });
    }

    public Mono<Void> deleteLegalEntity(String legalEntityExternalId) {
        return deleteLegalEntity(legalEntityExternalId, 10);
    }

    private Mono<LegalEntityTaskV2> upsertLegalEntity(LegalEntityTaskV2 task) {
        task.info(LEGAL_ENTITY, UPSERT, "", task.getData().getExternalId(), null, "Upsert Legal Entity with External ID: %s", task.getData().getExternalId());
        LegalEntityV2 legalEntity = task.getData();
        // Pipeline for Existing Legal Entity
        Mono<LegalEntityTaskV2> existingLegalEntity = legalEntityService.getLegalEntityByExternalId(legalEntity.getExternalId())
            .flatMap(actual -> {
                task.getData().setInternalId(actual.getInternalId());
                return legalEntityService.getLegalEntityByInternalId(actual.getInternalId())
                    .flatMap(result -> {
                        task.getData().setParentInternalId(result.getParentInternalId());

                        return legalEntityService.putLegalEntity(task.getData()).flatMap(leUpdated -> {
                            log.info("Updated LegalEntity: {}", leUpdated.getName());
                            task.info(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, UPDATED, legalEntity.getExternalId(), actual.getInternalId(), "Legal Entity: %s updated", legalEntity.getName());
                            return Mono.just(task);
                        });
                    });
            })
            .onErrorResume(throwable -> {
                if (throwable instanceof WebClientResponseException webClientResponseException) {
                    task.error(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, FAILED, legalEntity.getExternalId(), null,
                        webClientResponseException,
                        webClientResponseException.getResponseBodyAsString(), "Unexpected Web Client Exception");
                } else {
                    task.error(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, FAILED, legalEntity.getExternalId(), null, throwable,
                        throwable.getMessage(), "Unexpected Error");
                }
                return Mono.error(
                    new StreamTaskException(task, throwable, "Failed to get Legal Entity: " + throwable.getMessage()));
            });
        // Pipeline for Creating New Legal Entity
        Mono<LegalEntityTaskV2> createNewLegalEntity = Mono.defer(() -> legalEntityService.createLegalEntity(legalEntity)
            .flatMap(actual -> {
                task.getData().setInternalId(legalEntity.getInternalId());
                return legalEntityService.getLegalEntityByInternalId(actual.getInternalId())
                    .flatMap(result -> {
                        task.getData().setParentInternalId(result.getParentInternalId());
                        task.info(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, CREATED, legalEntity.getExternalId(),
                            legalEntity.getInternalId(), "Created new Legal Entity");
                        return Mono.just(task);
                    });
            })
            .onErrorResume(LegalEntityException.class, legalEntityException -> {
                task.error(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, FAILED, legalEntity.getExternalId(),
                    legalEntity.getInternalId(), legalEntityException, legalEntityException.getHttpResponse(),
                    legalEntityException.getMessage());
                return Mono.error(new StreamTaskException(task, legalEntityException));
            }));
        return existingLegalEntity.switchIfEmpty(createNewLegalEntity);
    }

    private Mono<LegalEntityTask> processProducts(LegalEntityTask streamTask) {
        LegalEntity legalEntity = streamTask.getData();
        if (legalEntity.getProductGroups() == null || legalEntity.getProductGroups().isEmpty()) {
            streamTask.info(LEGAL_ENTITY, PROCESS_PRODUCTS, FAILED, legalEntity.getInternalId(), legalEntity.getExternalId(), "Legal Entity: %s does not have any products defied", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }

        return Flux.fromIterable(legalEntity.getProductGroups())
            .map(actual -> createProductGroupTask(streamTask, actual))
            .concatMap(productGroupStreamTask -> batchProductIngestionSaga.process(productGroupStreamTask)
                .onErrorResume(throwable -> {
                    String message = throwable.getMessage();
                    if (throwable.getClass().isAssignableFrom(WebClientResponseException.class)) {
                        message = ((WebClientResponseException) throwable).getResponseBodyAsString();
                    }
                    streamTask.error(LEGAL_ENTITY, PROCESS_PRODUCTS, FAILED, legalEntity.getInternalId(), legalEntity.getExternalId(), throwable, message, "Unexpected error processing");
                    log.error("Unexpected error processing product group {}: {}", productGroupStreamTask.getData().getName(), message);
                    productGroupStreamTask.setState(StreamTask.State.FAILED);
                    return Mono.error(throwable);
                }))
            .map(productGroupTask -> streamTask.addHistory(productGroupTask.getHistory()))
            .collectList()
            .map(tasks -> {
                boolean failed = tasks.stream().anyMatch(StreamTask::isFailed);

                if (failed) {
                    streamTask.setState(StreamTask.State.FAILED);
                } else {
                    streamTask.setState(StreamTask.State.COMPLETED);
                }
                return streamTask;
            });
    }

    private ProductGroupTask createProductGroupTask(LegalEntityTask streamTask, ProductGroup productGroup) {
        LegalEntity legalEntity = streamTask.getData();

        if (productGroup.getUsers() == null) {
            productGroup.setUsers(streamTask.getData().getUsers());
        }
        StreamUtils.nullableCollectionToStream(productGroup.getUsers())
            .forEach(jobProfileUser -> {
                if (jobProfileUser.getLegalEntityReference() == null) {
                    jobProfileUser.setLegalEntityReference(new LegalEntityReference()
                        .externalId(legalEntity.getExternalId())
                        .internalId(legalEntity.getInternalId()));
                }
            });

        log.info("Setting up process data access groups");
        if (productGroup.getName() == null) {
            productGroup.setName(DEFAULT_DATA_GROUP);
        }
        if (productGroup.getDescription() == null) {
            productGroup.setDescription(DEFAULT_DATA_DESCRIPTION);
        }
        productGroup.setServiceAgreement(retrieveServiceAgreement(legalEntity));

        StreamUtils.getAllProducts(productGroup)
            .forEach((BaseProduct bp) -> {
                if (CollectionUtils.isEmpty(bp.getLegalEntities())
                    || bp.getLegalEntities().stream().map(LegalEntityReference::getExternalId).filter(Objects::nonNull)
                    .noneMatch(le -> le.equals(legalEntity.getExternalId()))) {
                    bp.addLegalEntitiesItem(new LegalEntityReference().externalId(legalEntity.getExternalId())
                        .internalId(legalEntity.getInternalId()));
                }
            });

        return new ProductGroupTask(streamTask.getId() + "-" + productGroup.getName(), productGroup);
    }

    private Mono<LegalEntityTask> createJobRoles(LegalEntityTask streamTask) {

        LegalEntity legalEntity = streamTask.getData();
        ServiceAgreement serviceAgreement = retrieveServiceAgreement(legalEntity);

        if (isEmpty(streamTask.getData().getReferenceJobRoles())
            && (serviceAgreement == null || isEmpty(serviceAgreement.getJobRoles()))) {
            log.debug("Skipping creation of job roles.");
            return Mono.just(streamTask);
        }

        log.info("Creating Job Roles...");

        return Flux.fromStream(Stream.of(serviceAgreement.getJobRoles(), legalEntity.getReferenceJobRoles())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream))
            .flatMap(jobRole -> accessGroupService.setupJobRole(streamTask, serviceAgreement, jobRole))
            .flatMap(jobRole -> {
                log.debug("Job Role: {}", jobRole.getName());
                return Mono.just(streamTask);
            })
            .collectList()
            .map(actual -> streamTask);
    }

    private Mono<LegalEntityTask> processJobProfiles(LegalEntityTask streamTask) {
        log.info("Processing Job Profiles for: {}", streamTask.getName());
        LegalEntity legalEntity = streamTask.getData();
        if (legalEntity.getUsers() == null) {
            streamTask.warn(BUSINESS_FUNCTION_GROUP, PROCESS_JOB_PROFILES, REJECTED, legalEntity.getExternalId(), legalEntity.getInternalId(), "No Job Profile Users defined in Legal Entity. No Business Function Groups will be assigned between a User and Legal Entity. ");
            return Mono.just(streamTask);
        }
        if (legalEntity.getUsers().stream().allMatch(jobProfileUser -> jobProfileUser.getUser() == null)) {
            streamTask.warn(BUSINESS_FUNCTION_GROUP, PROCESS_JOB_PROFILES, REJECTED, legalEntity.getExternalId(), legalEntity.getInternalId(), "No Users defined in Job Profiles");
            return Mono.just(streamTask);
        }
        return Flux.fromStream(nullableCollectionToStream(legalEntity.getUsers()))
            .flatMap(jobProfileUser -> {
                ServiceAgreement serviceAgreement = retrieveServiceAgreement(legalEntity);
                return getBusinessFunctionGroupTemplates(streamTask, jobProfileUser)
                    .flatMap(businessFunctionGroups -> accessGroupService.setupFunctionGroups(streamTask, serviceAgreement, businessFunctionGroups))
                    .flatMap(list -> {
                        log.info("Assigning {} Business Function Groups to Job Profile User: {}", list.size(), jobProfileUser.getUser().getExternalId());
                        jobProfileUser.setBusinessFunctionGroups(list);
                        list.forEach(bfg -> streamTask.info(BUSINESS_FUNCTION_GROUP, PROCESS_JOB_PROFILES, "assigned", legalEntity.getExternalId(), legalEntity.getInternalId(), "Assigned Business Function Group: %s with functions: %s to Service Agreement: %s", bfg.getName(),
                            ofNullable(bfg.getFunctions()).orElse(Collections.singletonList(new BusinessFunction().name("<not loaded>"))).stream().map(BusinessFunction::getFunctionCode).collect(Collectors.joining(", ")), serviceAgreement.getExternalId()));
                        return setupUserPermissions(streamTask, jobProfileUser);
                    })
                    .map(actual -> jobProfileUser);
            })
            .collectList()
            .map(jobProfileUsers -> {
                if (!jobProfileUsers.isEmpty())
                    streamTask.getData().setUsers(jobProfileUsers);
                return streamTask;
            });

    }

    private Mono<List<BusinessFunctionGroup>> getBusinessFunctionGroupTemplates(LegalEntityTask streamTask, JobProfileUser jobProfileUser) {
        streamTask.info(LEGAL_ENTITY, BUSINESS_FUNCTION_GROUP, "getBusinessFunctionGroupTemplates", "", "", "Using Reference Job Roles and Custom Job Roles defined in Job Profile User");
        List<BusinessFunctionGroup> businessFunctionGroups = jobProfileUser.getBusinessFunctionGroups();
        if (!isEmpty(jobProfileUser.getReferenceJobRoleNames())) {
            return accessGroupService.getFunctionGroupsForServiceAgreement(retrieveServiceAgreement(streamTask.getData()).getInternalId())
                .map(functionGroups -> {
                    Map<String, FunctionGroupItem> idByFunctionGroupName = functionGroups
                        .stream()
                        .filter(fg -> nonNull(fg.getId()))
                        .collect(Collectors.toMap(FunctionGroupItem::getName, Function.identity()));
                    return jobProfileUser.getReferenceJobRoleNames().stream()
                        .map(idByFunctionGroupName::get)
                        .filter(Objects::nonNull)
                        .map(businessFunctionGroupMapper::map)
                        .collect(Collectors.toList());
                })
                .map(bf -> {
                    if (!isEmpty(businessFunctionGroups))
                        bf.addAll(businessFunctionGroups);
                    return bf;
                });
        }
        return Mono.justOrEmpty(businessFunctionGroups);
    }

    private Mono<LegalEntityTaskV2> setupAdministrators(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        Flux<User> administrators = Flux.fromStream(nullableCollectionToStream(legalEntity.getAdministrators()));

        return administrators.flatMap(user -> upsertUser(streamTask, user))
            .collectList()
            .map(legalEntity::administrators)
            .map(streamTask::data);
    }

    private Mono<LegalEntityTaskV2>  setupUsers(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        Flux<User> userFlux = Flux.fromStream(nullableCollectionToStream(legalEntity.getUsers()));

        return userFlux
            .flatMap(user -> upsertUser(streamTask, user)
                .flatMap(upsertedUser -> {
                    User inputUser = nullableCollectionToStream(legalEntity.getUsers())
                        .filter(u -> u.getExternalId().equalsIgnoreCase(
                            upsertedUser.getExternalId()))
                        .findFirst().get();
                    inputUser.setInternalId(upsertedUser.getInternalId());
                    return upsertUserProfile(inputUser)
                        .flatMap(userProfile -> {
                            log.info("User Profile upserted for: {}", userProfile.getUserName());
                            inputUser.setUserProfile(userProfile);
                            return userService.getUserProfile(inputUser.getInternalId());
                        })
                        .doOnNext(userCacheProfile ->
                            log.info("User Cache Profile is existed: {}", userCacheProfile.getFullName())
                        );
                }))
            .collectList()
            .thenReturn(streamTask);
    }

    private Mono<LegalEntityTask> postUserContacts(LegalEntityTask streamTask, List<ExternalContact> externalContacts, String externalUserId) {
        if (isEmpty(externalContacts)) {
            log.info("Creating Contacts for User {}", externalUserId);
            streamTask.info(USER, PROCESS_CONTACTS, FAILED, externalUserId, null,
                    "User: %s does not have any Contacts", externalUserId);
            return Mono.just(streamTask);
        }
        LegalEntity legalEntity = streamTask.getData();
        log.info("Creating Contacts for User {}", externalUserId);
        return contactsSaga.executeTask(createContactsTask(streamTask.getId(), legalEntity.getExternalId(),
                null, externalUserId, AccessContextScope.USER, externalContacts))
                .flatMap(contactsTask -> requireNonNull(Mono.just(streamTask)))
                .then(Mono.just(streamTask));
    }

    private Mono<UserProfile> upsertUserProfile(User user) {
        if (legalEntitySagaConfigurationProperties.isUserProfileEnabled()) {
            if (user.getUserProfile() != null) {
                CreateUserProfile mappedUserProfile = userProfileMapper.toCreate(user);
                return userProfileService.upsertUserProfile(mappedUserProfile)
                    .map(userProfileMapper::toUserProfile);
            } else {
                log.debug("User Profile for {} is null. Skipping User Profile creation", user.getExternalId());
                return Mono.empty();
            }
        } else {
            log.debug("Skipping User Profile creation as config property is set to false");
            return Mono.empty();
        }
    }

    public Mono<LegalEntityTask> setupUserPermissions(LegalEntityTask legalEntityTask, JobProfileUser userJobProfile) {
        LegalEntity legalEntity = legalEntityTask.getData();
        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> request = Stream.of(userJobProfile)
            // Ensure internal Id present.
            .filter(jobProfileUser -> nonNull(jobProfileUser.getUser().getInternalId()))
            .collect(Collectors.toMap(
                JobProfileUser::getUser,
                jobProfileUser -> userJobProfile.getBusinessFunctionGroups().stream()
                    .collect(Collectors.toMap(
                        bfg -> bfg,
                        bfg -> Collections.emptyList()
                    ))
            ));
        log.trace("Permissions {}", request);
        return accessGroupService.assignPermissionsBatch(
                new BatchProductGroupTask(BATCH_PRODUCT_GROUP_ID + System.currentTimeMillis(), new BatchProductGroup()
                    .serviceAgreement(retrieveServiceAgreement(legalEntity)), legalEntityTask.getIngestionMode()), request)
            .thenReturn(legalEntityTask);
    }

    private User setupAdminInternalId(LegalEntity legalEntity, JobProfileUser jobProfileUser) {
        User user = jobProfileUser.getUser();
        // Ensure internal Id present.
        if (user.getInternalId() == null) {
            legalEntity.getAdministrators().stream()
                .filter(admin -> admin.getExternalId().equals(user.getExternalId()))
                .findFirst()
                .ifPresent(admin -> user.setInternalId(admin.getInternalId()));
        }
        return user;
    }

    public Mono<User> upsertUser(LegalEntityTaskV2 streamTask, User user) {
        if (legalEntitySagaConfigurationProperties.isUseIdentityIntegration()
            && !IdentityUserLinkStrategy.IDENTITY_AGNOSTIC.equals(user.getIdentityLinkStrategy())) {
            return upsertIdentityUser(streamTask, user);
        } else {
            log.debug("Fallback to Identity Agnostic identityLinkStrategy. Either identity integration is disabled or User identityLinkStrategy is not set to identity.");
            return upsertUser(user, streamTask);
        }
    }

    private Mono<User> upsertUserBulk(User user, LegalEntityTask streamTask) {
        LegalEntity legalEntity = streamTask.getData();
        streamTask.info(USER, UPSERT, "", user.getExternalId(), user.getInternalId(),
            "Upsert User with External ID: %s", user.getExternalId());

        Mono<User> getExistingUser = Mono.zip(Mono.just(user), userService.getUserByExternalId(user.getExternalId()), (u, existingUser) -> {
            u.setInternalId(existingUser.getInternalId());
            streamTask.info(USER, UPSERT, EXISTS, u.getExternalId(), u.getInternalId(), "User %s already exists",
                existingUser.getExternalId());
            return u;
        });

        Mono<User> createNewUser = Mono.zip(Mono.just(user), userService.createUser(user, legalEntity.getExternalId(), streamTask),
            (u, newUser) -> {
                u.setInternalId(newUser.getInternalId());
                streamTask.info(USER, UPSERT, CREATED, u.getExternalId(), user.getInternalId(), "User %s created",
                    newUser.getExternalId());
                return user;
            });
        return getExistingUser.switchIfEmpty(createNewUser);
    }

    private Mono<User> upsertUser(User user, LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        streamTask.info(USER, UPSERT, "", user.getExternalId(), "Upsert User with External ID: %s", user.getExternalId());

        Mono<User> existingUser = userService.getUserByExternalId(user.getExternalId()).flatMap(existUser -> {
            user.setInternalId(existUser.getInternalId());
            user.setLegalEntityId(existUser.getLegalEntityId());

            return userService.updateUser(user).flatMap(userUpdated -> {
                log.info("User was updated: {}", userUpdated.getFullName());
                streamTask.info(USER, UPSERT, UPDATED, user.getExternalId(), user.getInternalId(), "User %s updated",
                    existUser.getExternalId());
                return Mono.just(userUpdated);
            });
        });

        Mono<User> createNewUser = Mono.zip(Mono.just(user), userService.createUser(user, legalEntity.getExternalId(), streamTask),
            (u, newUser) -> {
                u.setInternalId(newUser.getInternalId());
                streamTask.info(USER, UPSERT, CREATED, u.getExternalId(), user.getInternalId(), "User %s created",
                    newUser.getExternalId());
                return user;
            });
        return existingUser.switchIfEmpty(createNewUser);
    }

    private Mono<User> upsertIdentityUser(LegalEntityTaskV2 streamTask, User user) {
      streamTask.info(IDENTITY_USER, UPSERT, "", user.getExternalId(), user.getInternalId(),
          "Upsert User to Identity with External ID: %s", user.getExternalId());
        LegalEntityV2 legalEntity = streamTask.getData();
        Mono<User> getExistingIdentityUser = userService.getUserByExternalId(user.getExternalId())
            .map(existingUser -> {
                user.setInternalId(existingUser.getInternalId());
                streamTask.info(IDENTITY_USER, UPSERT, EXISTS, user.getExternalId(), user.getInternalId(), "User %s already exists", existingUser.getExternalId());
                return user;
            })
            .flatMap(userService::updateIdentity);

        Mono<User> createNewIdentityUser =
            userService.createOrImportIdentityUser(user, legalEntity.getInternalId(), streamTask)
                .flatMap(currentUser -> userService.updateUserState(currentUser, legalEntity.getRealmName()))
                .map(existingUser -> {
                    user.setInternalId(existingUser.getInternalId());
                    streamTask.info(IDENTITY_USER, UPSERT, CREATED, user.getExternalId(), user.getInternalId(), "User %s created", existingUser.getExternalId());
                    return user;
                });
        return getExistingIdentityUser.switchIfEmpty(createNewIdentityUser);
    }

    private Mono<LegalEntityTaskV2> setupServiceAgreement(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();

        if (legalEntity.getMasterServiceAgreement() == null || StringUtils.hasText(legalEntity.getMasterServiceAgreement().getInternalId())) {

            Mono<LegalEntityTaskV2> existingServiceAgreement = legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(legalEntity.getInternalId())
                .flatMap(serviceAgreement -> {
                    if (legalEntity.getMasterServiceAgreement() != null ) {
                        serviceAgreement.setLimit(legalEntity.getMasterServiceAgreement().getLimit());
                        serviceAgreement.setParticipants(legalEntity.getMasterServiceAgreement().getParticipants());
                        if(legalEntity.getMasterServiceAgreement().getJobRoles() != null) {
                            serviceAgreement.setJobRoles(legalEntity.getMasterServiceAgreement().getJobRoles());
                        }
                    }
                    streamTask.getData().setMasterServiceAgreement(serviceAgreement);
                    streamTask.info(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, EXISTS, serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), "Existing Service Agreement: %s found for Legal Entity: %s", serviceAgreement.getExternalId(), legalEntity.getExternalId());
                    return Mono.just(streamTask);
                });

                // Master Service Agreement can be created only if activateSingleServiceAgreement property is missing or it has the value: true
                if (streamTask.getLegalEntity() != null &&
                    (streamTask.getLegalEntity().getActivateSingleServiceAgreement() == null || streamTask.getLegalEntity().getActivateSingleServiceAgreement())) {
                    ServiceAgreementV2 newServiceAgreement = createMasterServiceAgreement(legalEntity, legalEntity.getAdministrators());
                    Mono<LegalEntityTaskV2> createServiceAgreement = accessGroupService.createServiceAgreement(streamTask, newServiceAgreement)
                        .onErrorMap(AccessGroupException.class, accessGroupException -> {
                            streamTask.error(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, FAILED, newServiceAgreement.getExternalId(), null, accessGroupException, accessGroupException.getMessage(), accessGroupException.getHttpResponse());
                            return new StreamTaskException(streamTask, accessGroupException);
                        })
                        .flatMap(serviceAgreement -> {
                            streamTask.getData().setMasterServiceAgreement(serviceAgreement);
                            streamTask.info(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, CREATED, serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), "Created new Service Agreement: %s with Administrators: %s for Legal Entity: %s", serviceAgreement.getExternalId(), legalEntity.getAdministrators().stream().map(User::getExternalId).collect(Collectors.joining(", ")), legalEntity.getExternalId());
                            return Mono.just(streamTask);
                        });
                    return existingServiceAgreement.switchIfEmpty(createServiceAgreement);
                }
            return existingServiceAgreement;

        } else {
            return Mono.just(streamTask);
        }
    }

    private ServiceAgreementV2 createMasterServiceAgreement(LegalEntityV2 legalEntity, @Valid List<User> admins) {

        List<String> adminExternalIds = admins != null
            ? admins.stream().map(User::getExternalId).toList() :
            null;

        LegalEntityParticipantV2 legalEntityParticipant = new LegalEntityParticipantV2();
        legalEntityParticipant.setExternalId(legalEntity.getExternalId());
        legalEntityParticipant.setSharingAccounts(true);
        legalEntityParticipant.setSharingUsers(true);
        legalEntityParticipant.setAdmins(adminExternalIds);
        legalEntityParticipant.setUsers(Collections.emptyList());

        ServiceAgreementV2 serviceAgreement;

        if (legalEntity.getMasterServiceAgreement() == null) {
            serviceAgreement = new ServiceAgreementV2();
            serviceAgreement.setExternalId("sa_" + legalEntity.getExternalId());
            serviceAgreement.setName(legalEntity.getName());
            serviceAgreement.setDescription("Master Service Agreement for " + legalEntity.getName());
            serviceAgreement.setStatus(LegalEntityStatus.ENABLED);
        } else {
            serviceAgreement = legalEntity.getMasterServiceAgreement();
        }

        serviceAgreement.setIsMaster(true);
        if(isEmpty(serviceAgreement.getParticipants())) {
            serviceAgreement.addParticipantsItem(legalEntityParticipant);
        }

        return serviceAgreement;
    }

    private Mono<LegalEntityTaskV2> processSubsidiaries(LegalEntityTaskV2 streamTask) {

        LegalEntityV2 parentLegalEntity = streamTask.getData();

        Flux<LegalEntityV2> subsidiaries = parentLegalEntity.getSubsidiaries() == null
            ? Flux.empty()
            : Flux.fromIterable(parentLegalEntity.getSubsidiaries());

        Flux<LegalEntityTaskV2> subsidiariesWithParentId = setSubsidiaryParentLegalEntityId(parentLegalEntity,
            subsidiaries).map(LegalEntityTaskV2::new);
        return subsidiariesWithParentId
            .flatMap(this::executeTask)
            .map(childTask -> streamTask.addHistory(childTask.getHistory()))
            .collectList()
            .zipWith(Mono.just(streamTask), (legalEntityResults, childStreamTask) -> {
                // Do Something With The Children
                return streamTask;
            });
    }

    private Mono<LegalEntityTaskV2> linkLegalEntityToRealm(LegalEntityTaskV2 streamTask) {
        return Mono.just(streamTask)
            .filter(task -> legalEntitySagaConfigurationProperties.isUseIdentityIntegration())
            .flatMap(task ->
                userService.setupRealm(task.getLegalEntity())
                    .then(userService.linkLegalEntityToRealm(task.getLegalEntity()))
                    .map(legalEntity -> streamTask)
            ).switchIfEmpty(Mono.just(streamTask));
    }

    private ServiceAgreement retrieveServiceAgreement(LegalEntity legalEntity) {
        if (legalEntity.getCustomServiceAgreement() != null) {
            return legalEntity.getCustomServiceAgreement();
        }
        return legalEntity.getMasterServiceAgreement();
    }

    private Flux<LegalEntityV2> setSubsidiaryParentLegalEntityId(LegalEntityV2 parentLegalEntity,
                                                               Flux<LegalEntityV2> subsidiaries) {
        return subsidiaries.map(subsidiary -> {
            subsidiary.setParentExternalId(parentLegalEntity.getExternalId());
            return subsidiary;
        });
    }

    private Mono<LegalEntityTaskV2> setupLimits(LegalEntityTaskV2 streamTask) {
        return Mono.just(streamTask)
            .flatMap(this::setupLegalEntityLimits)
            .flatMap(this::setupLegalEntityLevelBusinessFunctionLimits);
    }

    private Mono<LegalEntityTaskV2> setupLegalEntityLimits(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        if(isNull(legalEntity.getLimit()) || !validateLimit(legalEntity.getLimit())) {
            streamTask.info(LEGAL_ENTITY, PROCESS_LIMITS, FAILED, legalEntity.getInternalId(), legalEntity.getExternalId(), "Legal Entity: %s does not have any Legal Entity limits defined", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }

        return limitsSaga.executeTask(createLimitsTask(streamTask, null, legalEntity.getInternalId(), legalEntity.getLimit()))
            .flatMap(limitsTask -> requireNonNull(Mono.just(streamTask)))
            .then(Mono.just(streamTask));
    }

    private boolean validateLimit(Limit limit) {

        return nonNull(limit) &&
                (nonNull(limit.getTransactional()) ||
                        nonNull(limit.getDaily()) ||
                        nonNull(limit.getWeekly()) ||
                        nonNull(limit.getMonthly()) ||
                        nonNull(limit.getQuarterly()) ||
                        nonNull(limit.getYearly()));
    }

    private Mono<LegalEntityTaskV2> setupLegalEntityLevelBusinessFunctionLimits(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        if (isNull(legalEntity.getLimit()) || isEmpty(legalEntity.getLimit().getBusinessFunctionLimits())) {
            streamTask.info(LEGAL_ENTITY, PROCESS_LIMITS, FAILED, legalEntity.getInternalId(), legalEntity.getExternalId(), "Legal Entity: %s does not have any Legal Entity limits defined", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }
        return Flux.fromStream(legalEntity.getLimit().getBusinessFunctionLimits()
                .stream()
                .filter(businessFunctionLimit -> nonNull(businessFunctionLimit)
                        && !CollectionUtils.isEmpty(businessFunctionLimit.getPrivileges()))
                .flatMap(businessFunctionLimit -> createLimitsTask(streamTask, legalEntity.getInternalId(), businessFunctionLimit)))
                .concatMap(limitsSaga::executeTask)
                .map(limitsTask -> streamTask.addHistory(limitsTask.getHistory()))
                .collectList()
                .map(tasks -> {
                    boolean failed = tasks.stream().anyMatch(StreamTask::isFailed);
                    if (failed) {
                        streamTask.setState(StreamTask.State.FAILED);
                    } else {
                        streamTask.setState(StreamTask.State.COMPLETED);
                    }
                    return streamTask;
                });
    }

    private Stream<LimitsTask> createLimitsTask(LegalEntityTaskV2 streamTask, String legalEntityId, BusinessFunctionLimit businessFunctionLimit) {

        if(isNull(businessFunctionLimit) || CollectionUtils.isEmpty(businessFunctionLimit.getPrivileges())){
            return Stream.of();
        }
        return businessFunctionLimit.getPrivileges()
            .stream()
            .filter(privilege -> validateLimit(privilege.getLimit()))
            .map(privilege -> {
                var limitData = new CreateLimitRequestBody();
                var entities = new ArrayList<Entity>();
                ofNullable(legalEntityId).ifPresent(le -> entities.add(new Entity().etype(LEGAL_ENTITY_E_TYPE).eref(le)));
                ofNullable(businessFunctionLimit.getFunctionId())
                        .ifPresent(functionId -> entities.add(new Entity().etype(FUNCTION_E_TYPE).eref(functionId)));
                ofNullable(privilege.getPrivilege())
                        .ifPresent(prv -> entities.add(new Entity().etype(PRIVILEGE_E_TYPE).eref(prv)));
                limitData.entities(entities);
                Optional.of(privilege)
                    .map(Privilege::getLimit).ifPresent(limit ->
                        limitData.periodicLimitsBounds(periodicLimits(limit))
                                .transactionalLimitsBound(transactionalLimits(limit))
                                .shadow(businessFunctionLimit.getShadow())
                                .currency(limit.getCurrencyCode()));
                return new LimitsTask(streamTask.getId() + "-" + LEGAL_ENTITY_LIMITS, limitData);
            });
    }

    private LimitsTask createLimitsTask(LegalEntityTaskV2 streamTask, ServiceAgreementV2 serviceAgreement, String legalEntityId, Limit limit) {

        var limitData = new CreateLimitRequestBody();
        var entities = new ArrayList<Entity>();
        ofNullable(legalEntityId).ifPresent(le -> entities.add(new Entity().etype(LEGAL_ENTITY_E_TYPE).eref(le)));
        ofNullable(serviceAgreement).ifPresent(
            sa -> entities.add(new Entity().etype(SERVICE_AGREEMENT_E_TYPE).eref(sa.getInternalId())));
        limitData.entities(entities);
        limitData.periodicLimitsBounds(periodicLimits(limit))
            .transactionalLimitsBound(transactionalLimits(limit))
            .currency(limit.getCurrencyCode());

        return new LimitsTask(streamTask.getId() + "-" + LEGAL_ENTITY_LIMITS, limitData);
    }

    private TransactionalLimitsBound transactionalLimits(Limit limit) {
        var transactionalLimits = new TransactionalLimitsBound();
        ofNullable(limit.getTransactional()).ifPresent(transactionalLimits::setAmount);
        return transactionalLimits;
    }

    private PeriodicLimitsBounds periodicLimits(Limit limit) {

        var periodicLimits = new PeriodicLimitsBounds();
        ofNullable(limit).ifPresent(l -> {
            ofNullable(l.getDaily()).ifPresent(periodicLimits::setDaily);
            ofNullable(l.getWeekly()).ifPresent(periodicLimits::setWeekly);
            ofNullable(l.getMonthly()).ifPresent(periodicLimits::setMonthly);
            ofNullable(l.getQuarterly()).ifPresent(periodicLimits::setDaily);
        });

        return periodicLimits;
    }
}
