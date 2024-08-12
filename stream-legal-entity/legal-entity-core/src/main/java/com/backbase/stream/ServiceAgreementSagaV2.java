package com.backbase.stream;

import static com.backbase.stream.product.utils.StreamUtils.nullableCollectionToStream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementParticipantsGetResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.Entity;
import com.backbase.dbs.limit.api.service.v2.model.PeriodicLimitsBounds;
import com.backbase.dbs.limit.api.service.v2.model.TransactionalLimitsBound;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.exceptions.AccessGroupException;
import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.Limit;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementUserAction;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.mapper.ExternalContactMapper;
import com.backbase.stream.mapper.ServiceAgreementV2ToV1Mapper;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.BusinessFunctionGroupMapper;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.streams.tailoredvalue.PlansService;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateRequestBody;
import io.micrometer.tracing.annotation.SpanTag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Agreement ingestion Saga. This Service can create Service Agreements
 * and their supporting objects from a {@link ServiceAgreementTaskV2} object.
 */
@Slf4j
public class ServiceAgreementSagaV2 implements StreamTaskExecutor<ServiceAgreementTaskV2> {

    public static final String LEGAL_ENTITY = "LEGAL_ENTITY";
    public static final String SERVICE_AGREEMENT = "SERVICE_AGREEMENT";
    public static final String BUSINESS_FUNCTION_GROUP = "BUSINESS_FUNCTION_GROUP";
    public static final String USER = "USER";
    private static final String DEFAULT_DATA_GROUP = "Default data group";
    private static final String DEFAULT_DATA_DESCRIPTION = "Default data group description";
    public static final String FAILED = "failed";
    public static final String EXISTS = "exists";
    public static final String CREATED = "created";
    public static final String SKIPPED = "skipped";

    public static final String PROCESS_PRODUCTS = "process-products";
    public static final String PROCESS_JOB_PROFILES = "process-job-profiles";
    public static final String PROCESS_LIMITS = "process-limits";
    public static final String PROCESS_CONTACTS = "process-contacts";
    public static final String PROCESS_PLANS = "process-plans";
    public static final String REJECTED = "rejected";
    public static final String SETUP_SERVICE_AGREEMENT = "setup-service-agreement";
    private static final String BATCH_PRODUCT_GROUP_ID = "batch_product_group_task-";

    private static final String LEGAL_ENTITY_E_TYPE = "LE";
    private static final String SERVICE_AGREEMENT_E_TYPE = "SA";
    private static final String FUNCTION_GROUP_E_TYPE = "FAG";
    private static final String FUNCTION_E_TYPE = "FUN";
    private static final String PRIVILEGE_E_TYPE = "PRV";
    private static final String JOB_ROLE_LIMITS = "job-role-limits";
    private static final String USER_JOB_ROLE_LIMITS = "user-job-role-limits";
    private static final String LEGAL_ENTITY_LIMITS = "legal-entity-limits";

    private final BusinessFunctionGroupMapper businessFunctionGroupMapper = Mappers.getMapper(BusinessFunctionGroupMapper.class);

    private final LegalEntityService legalEntityService;
    private final AccessGroupService accessGroupService;
    private final BatchProductIngestionSaga batchProductIngestionSaga;
    private final LimitsSaga limitsSaga;
    private final ContactsSaga contactsSaga;
    private final PlansService plansService;
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties;
    private static final ExternalContactMapper externalContactMapper = ExternalContactMapper.INSTANCE;
    private static final ServiceAgreementV2ToV1Mapper saMapper = ServiceAgreementV2ToV1Mapper.INSTANCE;

    public ServiceAgreementSagaV2(LegalEntityService legalEntityService,
        AccessGroupService accessGroupService,
        BatchProductIngestionSaga batchProductIngestionSaga,
        LimitsSaga limitsSaga,
        ContactsSaga contactsSaga,
        PlansService plansService,
        LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties) {
        this.legalEntityService = legalEntityService;
        this.accessGroupService = accessGroupService;
        this.batchProductIngestionSaga = batchProductIngestionSaga;
        this.limitsSaga = limitsSaga;
        this.contactsSaga = contactsSaga;
        this.plansService = plansService;
        this.legalEntitySagaConfigurationProperties = legalEntitySagaConfigurationProperties;
    }

    @Override
    public Mono<ServiceAgreementTaskV2> executeTask(@SpanTag(value = "streamTask") ServiceAgreementTaskV2 streamTask) {
        return setupServiceAgreement(streamTask)
            .flatMap(this::createJobRoles)
            .flatMap(this::retrieveUsersInternalIdsForJobProfile)
            .flatMap(this::processJobProfiles)
            .flatMap(this::setupAdministratorPermissions)
            .flatMap(this::setupLimits)
            .flatMap(this::processProducts)
            .flatMap(this::postContacts)
            .flatMap(this::updatePlans);
    }

    private Mono<ServiceAgreementTaskV2> updatePlans(ServiceAgreementTaskV2 streamTask){
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        if (!plansService.isEnabled()) {
            streamTask.info(SERVICE_AGREEMENT, PROCESS_PLANS, SKIPPED, serviceAgreement.getExternalId(), serviceAgreement.getInternalId(),
                    "Plan Saga configured to skipped");
            return Mono.just(streamTask);
        }
        log.info("Updating Plan for Service Agreement Id {}", serviceAgreement.getExternalId());
        serviceAgreement.getJobProfileUsers().stream().forEach(jobProfileUser -> {
            UserPlanUpdateRequestBody userPlanUpdateRequestBody = new UserPlanUpdateRequestBody();
            userPlanUpdateRequestBody.setId(""); // Plan id will be set internally by the saga
            userPlanUpdateRequestBody.serviceAgreementId(serviceAgreement.getInternalId());
            userPlanUpdateRequestBody.setLegalEntityId(jobProfileUser.getLegalEntityReference().getInternalId());
            plansService.updateUserPlan(jobProfileUser.getUser().getInternalId(),userPlanUpdateRequestBody,jobProfileUser.getPlanName() )
                    .flatMap(res -> requireNonNull(Mono.just(streamTask)));
        });
        return Mono.just(streamTask);
    }

    private Mono<ServiceAgreementTaskV2> postContacts(ServiceAgreementTaskV2 streamTask) {
        return postServiceAgreementContacts(streamTask)
            .flatMap(this::postUserContacts);
    }

    private Mono<ServiceAgreementTaskV2> postServiceAgreementContacts(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        if (isEmpty(serviceAgreement.getContacts())) {
            streamTask.info(SERVICE_AGREEMENT, PROCESS_CONTACTS, FAILED, serviceAgreement.getExternalId(), serviceAgreement.getInternalId(),
                "Service Agreement: %s does not have any Contacts defined", serviceAgreement.getExternalId());
            return Mono.just(streamTask);
        }
        log.info("Creating Contacts for Service Agreement Id {}", serviceAgreement.getExternalId());

        Optional<String> externalUserOptional = getUserExternalId(serviceAgreement.getJobProfileUsers());
        String externalUserId;
        if (externalUserOptional.isEmpty()) {
            externalUserId = getParticipantUser(saMapper.map(serviceAgreement));
            if (externalUserId == null) {
                streamTask.info(SERVICE_AGREEMENT, PROCESS_CONTACTS, FAILED, serviceAgreement.getExternalId(),
                    serviceAgreement.getInternalId(),
                    "SA: %s does not have any participants", serviceAgreement.getExternalId());
                return Mono.just(streamTask);
            }
        } else {
            externalUserId = externalUserOptional.get();
        }
        return contactsSaga.executeTask(
                createContactsTask(streamTask.getId(), getLegalEntityBySharingAccounts(serviceAgreement),
                    serviceAgreement.getExternalId(), externalUserId, AccessContextScope.SA,
                    serviceAgreement.getContacts()))
            .flatMap(contactsTask -> requireNonNull(Mono.just(streamTask)))
            .then(Mono.just(streamTask));
    }

    private String getLegalEntityBySharingAccounts(ServiceAgreementV2 serviceAgreement) {
        return serviceAgreement.getParticipants().stream()
            .filter(LegalEntityParticipant::getSharingAccounts).map(LegalEntityParticipant::getExternalId).findFirst()
            .orElseGet(() -> serviceAgreement.getParticipants().getFirst().getExternalId());
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

    private ContactsTask createContactsTask(String streamTaskId, String externalLegalEntityId,
        String externalServiceAgreementId, String externalUserId, AccessContextScope scope,
        List<ExternalContact> contacts) {
        var contactData = new ContactsBulkPostRequestBody();
        contactData.setIngestMode(IngestMode.UPSERT);
        contactData.setAccessContext(createExternalAccessContext(externalLegalEntityId, externalServiceAgreementId, externalUserId, scope));
        contactData.setContacts(externalContactMapper.toMapList(contacts));
        return new ContactsTask(streamTaskId + "-" + "contacts-task", contactData);
    }

    private ExternalAccessContext createExternalAccessContext(String externalLegalEntityId,
        String externalServiceAgreementId, String externalUserId, AccessContextScope scope) {
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
        return optionalUser.map(jobProfileUser -> jobProfileUser.getUser().getExternalId());
    }

    @Override
    public Mono<ServiceAgreementTaskV2> rollBack(ServiceAgreementTaskV2 streamTask) {
        // GET CREATED AND EVENTS AND CALL DELETE ENDPOINTS IN REVERSE
        return Mono.just(streamTask);
    }

    private Mono<ServiceAgreementTaskV2> processProducts(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        if (serviceAgreement.getProductGroups() == null || serviceAgreement.getProductGroups().isEmpty()) {
            streamTask.info(SERVICE_AGREEMENT, PROCESS_PRODUCTS, FAILED, serviceAgreement.getInternalId(),
                serviceAgreement.getExternalId(),
                "Service agreement: %s does not have any products defied", serviceAgreement.getExternalId());
            return Mono.just(streamTask);
        }
        log.info("Creating Arrangements for Service Agreement Id {}", serviceAgreement.getExternalId());

        return Flux.fromIterable(serviceAgreement.getProductGroups())
            .mapNotNull(actual -> createProductGroupTask(streamTask, actual))
            .concatMap(productGroupStreamTask -> batchProductIngestionSaga.process(productGroupStreamTask)
                .onErrorResume(throwable -> {
                    String message = throwable.getMessage();
                    if (throwable.getClass().isAssignableFrom(WebClientResponseException.class)) {
                        message = ((WebClientResponseException) throwable).getResponseBodyAsString();
                    }
                    streamTask.error(SERVICE_AGREEMENT, PROCESS_PRODUCTS, FAILED, serviceAgreement.getInternalId(),
                        serviceAgreement.getExternalId(), throwable, message, "Unexpected error processing");
                    log.error("Unexpected error processing product group {}: {}",
                        productGroupStreamTask.getData().getName(), message);
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

    private ProductGroupTask createProductGroupTask(ServiceAgreementTaskV2 streamTask, ProductGroup productGroup) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();

        if (productGroup.getUsers() == null) {
            log.error("Product group {} does not have users", productGroup.getName());
            return null;
        }

        log.info("Setting up process data access groups");
        if (productGroup.getName() == null) {
            productGroup.setName(DEFAULT_DATA_GROUP);
        }
        if (productGroup.getDescription() == null) {
            productGroup.setDescription(DEFAULT_DATA_DESCRIPTION);
        }
        productGroup.setServiceAgreement(saMapper.map(serviceAgreement));

        List<String> errors = new ArrayList<>();
        StreamUtils.getAllProducts(productGroup)
            .forEach((BaseProduct bp) -> {
                if (CollectionUtils.isEmpty(bp.getLegalEntities())) {
                    errors.add("Product: " + bp.getExternalId());
                }
            });
        if (!CollectionUtils.isEmpty(errors)) {
            throw new IllegalArgumentException("Products does not have legalEntities defined: " + String.join(",", errors));
        }

        return new ProductGroupTask(streamTask.getId() + "-" + productGroup.getName(), productGroup);

    }

    private Mono<ServiceAgreementTaskV2> createJobRoles(ServiceAgreementTaskV2 streamTask) {

        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();

        if (isEmpty(serviceAgreement.getReferenceJobRoles()) && isEmpty(serviceAgreement.getJobRoles())) {
            log.debug("Skipping creation of job roles, no reference job roles or job roles are present.");
            return Mono.just(streamTask);
        }

        log.info("Creating Job Roles...");

        return Flux.fromStream(Stream.of(serviceAgreement.getJobRoles(), serviceAgreement.getReferenceJobRoles())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream))
            .flatMap(jobRole -> accessGroupService.setupJobRole(streamTask, saMapper.map(serviceAgreement),
                jobRole))
            .flatMap(jobRole -> {
                log.debug("Job Role: {}", jobRole.getName());
                return Mono.just(streamTask);
            })
            .collectList()
            .map(actual -> streamTask);
    }

    private Mono<ServiceAgreementTaskV2> processJobProfiles(ServiceAgreementTaskV2 streamTask) {
        log.info("Processing Job Profiles for: {}", streamTask.getName());
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();

        if (serviceAgreement.getJobProfileUsers() == null) {
            streamTask.warn(BUSINESS_FUNCTION_GROUP, PROCESS_JOB_PROFILES, REJECTED,
                serviceAgreement.getExternalId(), serviceAgreement.getInternalId(),
                "No Job Profile Users defined in Service agreement. No Business Function Groups will be assigned.");
            return Mono.just(streamTask);
        }
        if (serviceAgreement.getJobProfileUsers().stream().allMatch(jobProfileUser -> jobProfileUser.getUser() == null)) {
            streamTask.warn(BUSINESS_FUNCTION_GROUP, PROCESS_JOB_PROFILES, REJECTED, serviceAgreement.getExternalId(),
                serviceAgreement.getInternalId(), "No Users defined in Job Profiles");
            return Mono.just(streamTask);
        }
        return Flux.fromStream(nullableCollectionToStream(serviceAgreement.getJobProfileUsers()))
            .flatMap(jobProfileUser -> getBusinessFunctionGroupTemplates(streamTask, jobProfileUser)
                .flatMap(businessFunctionGroups -> accessGroupService.setupFunctionGroups(streamTask,
                    saMapper.map(streamTask.getServiceAgreement()), businessFunctionGroups))
                .flatMap(list -> {
                    log.info("Assigning {} Business Function Groups to Job Profile User: {}", list.size(),
                        jobProfileUser.getUser().getExternalId());
                    jobProfileUser.setBusinessFunctionGroups(list);
                    list.forEach(bfg -> streamTask.info(BUSINESS_FUNCTION_GROUP, PROCESS_JOB_PROFILES, "assigned",
                        serviceAgreement.getExternalId(), serviceAgreement.getInternalId(),
                        "Assigned Business Function Group: %s with functions: %s to Service Agreement: %s",
                        bfg.getName(),
                        ofNullable(bfg.getFunctions())
                            .orElse(Collections.singletonList(new BusinessFunction().name("<not loaded>"))).stream()
                            .map(BusinessFunction::getFunctionCode)
                            .collect(Collectors.joining(", ")),
                        serviceAgreement.getExternalId()));
                    return setupUserPermissions(streamTask, jobProfileUser);
                })
                .map(actual -> jobProfileUser))
            .collectList()
            .map(jobProfileUsers -> {
                if (!jobProfileUsers.isEmpty())
                    serviceAgreement.setJobProfileUsers(jobProfileUsers);
                return streamTask;
            });

    }

    private Mono<List<BusinessFunctionGroup>> getBusinessFunctionGroupTemplates(ServiceAgreementTaskV2 streamTask, JobProfileUser jobProfileUser) {
        streamTask.info(LEGAL_ENTITY, BUSINESS_FUNCTION_GROUP, "getBusinessFunctionGroupTemplates", "",
            "", "Using Reference Job Roles and Custom Job Roles defined in Job Profile User");
        List<BusinessFunctionGroup> businessFunctionGroups = jobProfileUser.getBusinessFunctionGroups();
        if (!isEmpty(jobProfileUser.getReferenceJobRoleNames())) {
            return accessGroupService.getFunctionGroupsForServiceAgreement(streamTask.getServiceAgreement().getInternalId())
                .map(functionGroups -> {
                    Map<String, FunctionGroupItem> idByFunctionGroupName = functionGroups
                        .stream()
                        .filter(fg -> nonNull(fg.getId()))
                        .collect(Collectors.toMap(FunctionGroupItem::getName, Function.identity()));
                    return jobProfileUser.getReferenceJobRoleNames().stream()
                        .map(idByFunctionGroupName::get)
                        .filter(Objects::nonNull)
                        .map(businessFunctionGroupMapper::map)
                        .toList();
                })
                .map(bf -> {
                    if (!isEmpty(businessFunctionGroups))
                        bf.addAll(businessFunctionGroups);
                    return bf;
                })
                .doOnError(throwable -> log.error("error fetching function group for service agreement {} with error {}",
                    streamTask.getServiceAgreement().getInternalId(), throwable.getMessage()));
        }
        return Mono.justOrEmpty(CollectionUtils.isEmpty(businessFunctionGroups) ? null : businessFunctionGroups);
    }

    private Mono<ServiceAgreementTaskV2> postUserContacts(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();

        Flux<JobProfileUser> jobProfileUsers = Flux.fromStream(nullableCollectionToStream(
            serviceAgreement.getJobProfileUsers()));
        return jobProfileUsers
            .flatMap(jobProfileUser -> postUserContacts(streamTask, jobProfileUser.getContacts(),
                jobProfileUser.getUser().getExternalId(), jobProfileUser.getLegalEntityReference().getExternalId()))
            .collectList()
            .thenReturn(streamTask);

    }

    private Mono<ServiceAgreementTaskV2> postUserContacts(ServiceAgreementTaskV2 streamTask,
        List<ExternalContact> externalContacts, String externalUserId, String leId) {
        if (isEmpty(externalContacts)) {
            log.info("User {} has no contacts", externalUserId);
            streamTask.info(USER, PROCESS_CONTACTS, FAILED, externalUserId, null,
                "User: %s does not have any Contacts", externalUserId);
            return Mono.just(streamTask);
        }
        log.info("Creating Contacts for User {}", externalUserId);
        return contactsSaga.executeTask(createContactsTask(streamTask.getId(), leId,
                null, externalUserId, AccessContextScope.USER, externalContacts))
            .flatMap(contactsTask -> requireNonNull(Mono.just(streamTask)))
            .then(Mono.just(streamTask));
    }

    public Mono<ServiceAgreementTaskV2> setupUserPermissions(ServiceAgreementTaskV2 serviceAgreementTaskV2,
        JobProfileUser userJobProfile) {
        ServiceAgreementV2 serviceAgreement = serviceAgreementTaskV2.getServiceAgreement();
        log.info("Setup user permissions for user: {}", userJobProfile.getUser().getExternalId());
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

        if (request.isEmpty()) {
            log.info("Skipping setup of permissions since no declarative business functions were found.");
            return Mono.just(serviceAgreementTaskV2);
        }

        return accessGroupService.assignPermissionsBatch(
                new BatchProductGroupTask(BATCH_PRODUCT_GROUP_ID + System.currentTimeMillis(), new BatchProductGroup()
                    .serviceAgreement(saMapper.map(serviceAgreement)),
                    serviceAgreementTaskV2.getIngestionMode()), request)
            .thenReturn(serviceAgreementTaskV2);
    }

    public Mono<ServiceAgreementTaskV2> setupAdministratorPermissions(ServiceAgreementTaskV2 streamTask) {
        // Assign permissions for the user for all business function groups.
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> request = nullableCollectionToStream(
            serviceAgreement.getJobProfileUsers())
            .filter(jobProfileUser -> !isEmpty(jobProfileUser.getBusinessFunctionGroups()))
            .collect(Collectors.toMap(
                JobProfileUser::getUser,
                jobProfileUser -> jobProfileUser.getBusinessFunctionGroups().stream()
                    .collect(Collectors.toMap(
                        Function.identity(), // Specify the type explicitly
                        bfg -> Collections.emptyList()
                    ))
            ));

        if (request.isEmpty()) {
            log.info("Skipping setup of permissions since no declarative business functions were found.");
            return Mono.just(streamTask);
        }

        return accessGroupService.assignPermissionsBatch(
                new BatchProductGroupTask(BATCH_PRODUCT_GROUP_ID + System.currentTimeMillis(), new BatchProductGroup()
                    .serviceAgreement(saMapper.map(serviceAgreement)), BatchProductIngestionMode.UPSERT),
                request)
            .thenReturn(streamTask);
    }

    private Mono<ServiceAgreementTaskV2> setupServiceAgreement(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 sa = streamTask.getServiceAgreement();

        log.info("Starting setup of Service Agreement with id {}", sa.getExternalId());
        if (sa.getIsMaster() == null || !sa.getIsMaster()) {
            return setupCustomServiceAgreement(streamTask);
        } else {
            if (StringUtils.isNotEmpty(sa.getInternalId())) {
                return Mono.just(streamTask);
            }
            Optional<LegalEntityParticipant> legalEntityParticipant = sa.getParticipants().stream().findFirst();
            if (legalEntityParticipant.isEmpty()) {
                log.info("Skipping setup of master service agreement which has no participants.");
                return Mono.just(streamTask);
            }
            ServiceAgreement serviceAgreementV1 = saMapper.map(sa);
            Mono<ServiceAgreementTaskV2> existingServiceAgreement =
                legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(legalEntityParticipant.get().getExternalId())
                    .flatMap(serviceAgreement -> {
                        serviceAgreement.setLimit(serviceAgreementV1.getLimit());
                        serviceAgreement.setParticipants(serviceAgreementV1.getParticipants());
                        if (serviceAgreementV1.getJobRoles() != null) {
                            serviceAgreement.setJobRoles(serviceAgreementV1.getJobRoles());
                        }
                        streamTask.info(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, EXISTS,
                            serviceAgreement.getExternalId(), serviceAgreement.getInternalId(),
                            "Existing Master Service Agreement: %s found for Legal Entity: %s",
                            serviceAgreement.getExternalId(), legalEntityParticipant.get().getExternalId());
                        ServiceAgreementV2 serviceAgreementV2 = saMapper.mapV2(serviceAgreement);
                        serviceAgreementV2.setProductGroups(sa.getProductGroups());
                        serviceAgreementV2.setReferenceJobRoles(sa.getReferenceJobRoles());
                        serviceAgreementV2.setJobProfileUsers(sa.getJobProfileUsers());
                        streamTask.setServiceAgreement(serviceAgreementV2);
                        return Mono.just(streamTask);
                    });

            ServiceAgreement newServiceAgreement = createMasterServiceAgreement(serviceAgreementV1);
            Mono<ServiceAgreementTaskV2> createServiceAgreement = accessGroupService.createServiceAgreement(streamTask, newServiceAgreement)
                .onErrorMap(AccessGroupException.class, accessGroupException -> {
                    streamTask.error(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, FAILED,
                        newServiceAgreement.getExternalId(), null, accessGroupException,
                        accessGroupException.getMessage(), accessGroupException.getHttpResponse());
                    return new StreamTaskException(streamTask, accessGroupException);
                })
                .flatMap(serviceAgreement -> {
                    streamTask.getServiceAgreement().getParticipants().get(0).setSharingAccounts(true);
                    streamTask.getServiceAgreement().getParticipants().get(0).setSharingUsers(true);
                    streamTask.info(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, CREATED,
                        serviceAgreement.getExternalId(), serviceAgreement.getInternalId(),
                        "Created new Service Agreement: %s", serviceAgreement.getExternalId(),
                        serviceAgreement.getExternalId());
                    streamTask.getServiceAgreement().setInternalId(serviceAgreement.getInternalId());
                    return Mono.just(streamTask);
                });
            return existingServiceAgreement.switchIfEmpty(createServiceAgreement);
        }
    }

    private Mono<ServiceAgreementTaskV2> setupCustomServiceAgreement(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreementV2 = streamTask.getServiceAgreement();
        if (serviceAgreementV2.getExternalId() == null) {
            log.error("Defined service agreement contains no external Id");
            return Mono.error(new StreamTaskException(streamTask, "Defined service agreement contains no external Id"));
        }

        List<ServiceAgreementUserAction> userActions = nullableCollectionToStream(serviceAgreementV2
            .getJobProfileUsers())
            .map(JobProfileUser::getUser).map(User::getExternalId)
            .map(id -> new ServiceAgreementUserAction().action(ServiceAgreementUserAction.ActionEnum.ADD)
                .userProfile(new JobProfileUser().user(new User().externalId(id)))).toList();

        Mono<ServiceAgreementTaskV2> existingServiceAgreement = accessGroupService
            .getServiceAgreementByExternalId(serviceAgreementV2.getExternalId())
            .flatMap(sa -> {
                serviceAgreementV2.setInternalId(sa.getInternalId());
                streamTask.info(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, EXISTS, sa.getExternalId(), sa.getInternalId(),
                    "Existing Service Agreement: %s found for Legal Entities: %s", sa.getExternalId(),
                    serviceAgreementV2.getParticipants().stream()
                        .map(LegalEntityParticipant::getExternalId)
                        .collect(Collectors.joining(", ")));
                setLECreator4SA(serviceAgreementV2);
                if (legalEntitySagaConfigurationProperties.isServiceAgreementUpdateEnabled()) {
                    return accessGroupService.updateServiceAgreementItem(streamTask, saMapper.map(serviceAgreementV2))
                        .then(accessGroupService.updateServiceAgreementAssociations(streamTask,
                            saMapper.map(serviceAgreementV2), userActions))
                        .thenReturn(streamTask);
                } else {
                    return accessGroupService.updateServiceAgreementAssociations(streamTask,
                            saMapper.map(serviceAgreementV2), userActions)
                        .thenReturn(streamTask);
                }
            });

        return createServiceAgreementTaskV2(streamTask, serviceAgreementV2, userActions, existingServiceAgreement);
    }

    @NotNull
    private Mono<ServiceAgreementTaskV2> createServiceAgreementTaskV2(ServiceAgreementTaskV2 streamTask,
        ServiceAgreementV2 serviceAgreementV2, List<ServiceAgreementUserAction> userActions,
        Mono<ServiceAgreementTaskV2> existingServiceAgreement) {
        Mono<ServiceAgreementTaskV2> createServiceAgreement = accessGroupService.createServiceAgreement(streamTask,
                saMapper.map(serviceAgreementV2))
            .onErrorMap(AccessGroupException.class, accessGroupException -> {
                streamTask.error(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, FAILED, serviceAgreementV2.getExternalId(), null,
                    accessGroupException, accessGroupException.getMessage(),
                    accessGroupException.getHttpResponse());
                return new StreamTaskException(streamTask, accessGroupException);
            })
            .flatMap(createdSa -> {
                serviceAgreementV2.setInternalId(createdSa.getInternalId());
                streamTask.info(SERVICE_AGREEMENT, SETUP_SERVICE_AGREEMENT, CREATED, createdSa.getExternalId(),
                    createdSa.getInternalId(),
                    "Created new Service Agreement: %s",
                    createdSa.getExternalId(),
                        serviceAgreementV2.getParticipants().stream()
                            .map(LegalEntityParticipant::getExternalId)
                            .collect(Collectors.joining(", ")));
                setLECreator4SA(serviceAgreementV2);

                return accessGroupService.updateServiceAgreementRegularUsers(streamTask,
                        saMapper.map(serviceAgreementV2), userActions)
                    .doOnError(throwable -> log.error("error updating service agreement regular users {}", throwable.getMessage()))
                    .thenReturn(streamTask);
            });

        return existingServiceAgreement.switchIfEmpty(createServiceAgreement);
    }


    private void setLECreator4SA(ServiceAgreementV2 serviceAgreementV2) {
        //unlike LegalEntitySaga, we simply check if there is a creatorLegalEntity specified, and if it is
        //external Id, we change it to relevant internal id
        if (StringUtils.isNotEmpty(serviceAgreementV2.getCreatorLegalEntity())) {
            legalEntityService.getLegalEntityByExternalId(serviceAgreementV2.getCreatorLegalEntity())
                .subscribe(le -> serviceAgreementV2.setCreatorLegalEntity(le.getInternalId()));
        }
    }

    private ServiceAgreement createMasterServiceAgreement(ServiceAgreement serviceAgreement) {

        serviceAgreement.getParticipants().get(0).setSharingAccounts(true);
        serviceAgreement.getParticipants().get(0).setSharingUsers(true);

        return serviceAgreement;
    }

    private Mono<ServiceAgreementTaskV2> setupLimits(ServiceAgreementTaskV2 streamTask) {
        return setupServiceAgreementLimits(streamTask)
            .flatMap(this::setupServiceAgreementParticipantLimits)
            .flatMap(this::retrieveUsersInternalIds)
            .flatMap(this::setupJobRoleLimits);
    }

    private Mono<ServiceAgreementTaskV2> setupServiceAgreementLimits(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 sa = streamTask.getServiceAgreement();
        ServiceAgreement serviceAgreement = saMapper.map(sa);
        if (isNull(serviceAgreement.getLimit())) {
            streamTask.info(SERVICE_AGREEMENT, PROCESS_LIMITS, FAILED,
                sa.getInternalId(), sa.getExternalId(),
                "Service agreement: %s does not have any limits defined", serviceAgreement.getExternalId());
            return Mono.just(streamTask);
        }
        return limitsSaga.executeTask(createLimitsTask(streamTask, serviceAgreement, null, serviceAgreement.getLimit()))
            .flatMap(limitsTask -> requireNonNull(Mono.just(streamTask)))
            .then(Mono.just(streamTask));
    }

    private LimitsTask createLimitsTask(ServiceAgreementTaskV2 streamTask, ServiceAgreement serviceAgreement, String legalEntityId, Limit limit) {

        var limitData = new CreateLimitRequestBody();
        var entities = new ArrayList<Entity>();
        ofNullable(legalEntityId).ifPresent(le -> entities.add(new Entity().etype(LEGAL_ENTITY_E_TYPE).eref(le)));
        ofNullable(serviceAgreement).ifPresent(sa -> entities.add(new Entity().etype(SERVICE_AGREEMENT_E_TYPE).eref(sa.getInternalId())));
        limitData.entities(entities);
        limitData.periodicLimitsBounds(periodicLimits(limit))
            .transactionalLimitsBound(transactionalLimits(limit))
            .currency(limit.getCurrencyCode());

        return new LimitsTask(streamTask.getId() + "-" + LEGAL_ENTITY_LIMITS, limitData);
    }

    private Mono<ServiceAgreementTaskV2> setupServiceAgreementParticipantLimits(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 saV2 = streamTask.getServiceAgreement();
        ServiceAgreement serviceAgreement = saMapper.map(saV2);
        if(isNull(serviceAgreement.getParticipants())
            || serviceAgreement.getParticipants().stream().noneMatch(legalEntityParticipant -> legalEntityParticipant.getLimit() != null)) {
            streamTask.info(SERVICE_AGREEMENT, PROCESS_LIMITS, FAILED, serviceAgreement.getInternalId(),
                serviceAgreement.getExternalId(),
                "SA: %s does not have any Participant with Limits in Service Agreement", serviceAgreement.getExternalId());
            return Mono.just(streamTask);
        }
        return accessGroupService.getServiceAgreementParticipants(streamTask, serviceAgreement)
            .filter(participant ->  serviceAgreement.getParticipants().stream().filter(p -> p.getExternalId().equalsIgnoreCase(participant.getExternalId())).anyMatch(legalEntityParticipant -> legalEntityParticipant.getLimit() != null))
            .flatMapIterable(participant -> List.of(createLimitsTask(streamTask, serviceAgreement, participant.getId(), getLimits(serviceAgreement, participant))))
            .flatMap(limitsSaga::executeTask)
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

    private Limit getLimits(ServiceAgreement serviceAgreement, ServiceAgreementParticipantsGetResponseBody participant) {
        return serviceAgreement.getParticipants().stream()
            .filter(legalEntityParticipant -> legalEntityParticipant.getExternalId().equalsIgnoreCase(participant.getExternalId()))
            .map(LegalEntityParticipant::getLimit)
            .findFirst().orElseGet(Limit::new);
    }

    @NotNull
    private Mono<ServiceAgreementTaskV2> setupJobRoleLimits(ServiceAgreementTaskV2 streamTask) {

        ServiceAgreementV2 saV2 = streamTask.getServiceAgreement();
        ServiceAgreement serviceAgreement = saMapper.map(saV2);
        if (noLimitsInJobRole(saV2.getJobRoles())
            && noLimitsInJobRole(saV2.getReferenceJobRoles())) {
            streamTask.info(SERVICE_AGREEMENT, PROCESS_LIMITS, FAILED, serviceAgreement.getInternalId(),
                serviceAgreement.getExternalId(), "SA: %s does not have any Job Role limits defined",
                serviceAgreement.getExternalId());
            return Mono.just(streamTask);
        }

        Map<String, Set<String>> userJobRoleMap = new HashMap<>();
        if (saV2.getProductGroups() != null) {
            saV2.getProductGroups().stream()
                .filter(productGroup -> nonNull(productGroup.getUsers()))
                .flatMap(productGroup -> productGroup.getUsers().stream())
                .filter(jobProfileUser -> nonNull(jobProfileUser.getUser()) && nonNull(
                    jobProfileUser.getUser().getSupportsLimit()) && !isEmpty(jobProfileUser.getReferenceJobRoleNames()))
                .forEach(jobProfileUser -> jobProfileUser.getReferenceJobRoleNames().forEach(jobRoleName -> {
                    if(userJobRoleMap.get(jobRoleName) != null) {
                        var users = userJobRoleMap.get(jobRoleName);
                        users.add(jobProfileUser.getUser().getInternalId());
                        userJobRoleMap.put(jobRoleName, users);
                    } else {
                        userJobRoleMap.put(jobRoleName, new HashSet<>(List.of(jobProfileUser.getUser().getInternalId())));
                    }
                }));
        }

        return Flux.fromStream(Stream.of(saV2.getJobRoles(), saV2.getReferenceJobRoles())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream))
            .flatMapIterable(actual -> createLimitsTask(streamTask, actual, serviceAgreement, userJobRoleMap))
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

    private Mono<ServiceAgreementTaskV2> retrieveUsersInternalIds(ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        if (serviceAgreement.getProductGroups() == null
            || serviceAgreement.getProductGroups().stream().allMatch(productGroup -> Objects.isNull(productGroup.getUsers()))
            || serviceAgreement.getProductGroups().stream().filter(productGroup -> nonNull(productGroup.getUsers()))
            .flatMap(productGroup -> productGroup.getUsers().stream())
            .noneMatch(jobProfileUser -> nonNull(jobProfileUser)
                && nonNull(jobProfileUser.getUser())
                && nonNull(jobProfileUser.getUser().getSupportsLimit())
                && jobProfileUser.getUser().getSupportsLimit())) {
            return Mono.just(streamTask);
        }

        var users = serviceAgreement.getProductGroups().stream()
            .flatMap(productGroup -> productGroup.getUsers().stream()).collect(Collectors.toSet());
        return Flux.fromIterable(users)
            .flatMap(jpu -> accessGroupService.getUserByExternalId(jpu.getUser().getExternalId(), true))
            .collectList()
            .flatMap(internalUsers -> {
                Map<String, GetUser> usersByExternalId =
                    internalUsers.stream().collect(Collectors.toMap(GetUser::getExternalId, Function.identity(), (a1, a2) -> a1));
                users.forEach(jp -> {
                    String externalId = jp.getUser().getExternalId();
                    GetUser internalUser = usersByExternalId.get(externalId);
                    if (internalUser != null) {
                        jp.getUser().setInternalId(internalUser.getId());
                    }
                });
                return Mono.just(streamTask);
            });
    }

    private List<LimitsTask> createLimitsTask(ServiceAgreementTaskV2 streamTask, JobRole actual,
        ServiceAgreement serviceAgreement, Map<String, Set<String>> userJobRoleMap) {

        List<LimitsTask> userJobRoleLimits = new ArrayList<>();
        if(!CollectionUtils.isEmpty(userJobRoleMap) && userJobRoleMap.containsKey(actual.getName())) {
            userJobRoleMap.get(actual.getName()).forEach(userId -> userJobRoleLimits.addAll(actual.getFunctionGroups().stream()
                .filter(this::limitsExist)
                .flatMap(businessFunctionGroup -> businessFunctionGroup.getFunctions().stream()
                    .flatMap(businessFunction -> businessFunction.getPrivileges().stream()
                        .filter(privilege -> nonNull(privilege.getLimit()))
                        .filter(privilege -> nonNull(privilege.getLimit().getCurrencyCode()))
                        .filter(this::atLeastOneLimitExist)
                        .map(privilege -> getCreateLimitRequestBody(serviceAgreement,
                            businessFunction, privilege, actual.getId(), userId))))
                .map(limitData -> new LimitsTask(streamTask.getId() + "-" + USER_JOB_ROLE_LIMITS, limitData))
                .toList()));
        }

        var jobRoleLimits = actual.getFunctionGroups().stream()
            .filter(this::limitsExist)
            .flatMap(businessFunctionGroup -> businessFunctionGroup.getFunctions().stream()
                .flatMap(businessFunction -> businessFunction.getPrivileges().stream()
                    .filter(privilege -> nonNull(privilege.getLimit()))
                    .filter(privilege -> nonNull(privilege.getLimit().getCurrencyCode()))
                    .filter(this::atLeastOneLimitExist)
                    .map(privilege -> getCreateLimitRequestBody(serviceAgreement,
                        businessFunction, privilege, actual.getId(), null))))
            .map(limitData -> new LimitsTask(streamTask.getId() + "-" + JOB_ROLE_LIMITS, limitData))
            .toList();

        return Stream.concat(jobRoleLimits.stream(), userJobRoleLimits.stream()).toList();
    }

    @NotNull
    private CreateLimitRequestBody getCreateLimitRequestBody(ServiceAgreement serviceAgreement,
        BusinessFunction businessFunction, Privilege privilege, String fagId, String userId) {
        CreateLimitRequestBody request = new CreateLimitRequestBody();
        request.setUserBBID(userId);
        request.entities(List.of(new Entity().etype(SERVICE_AGREEMENT_E_TYPE).eref(serviceAgreement.getInternalId()),
            new Entity().etype(FUNCTION_GROUP_E_TYPE).eref(fagId),
            new Entity().etype(FUNCTION_E_TYPE).eref(businessFunction.getFunctionId()),
            new Entity().etype(PRIVILEGE_E_TYPE).eref(privilege.getPrivilege().toLowerCase())));
        request.periodicLimitsBounds(periodicLimits(privilege.getLimit()))
            .transactionalLimitsBound(transactionalLimits(privilege.getLimit()))
            .currency(privilege.getLimit().getCurrencyCode());
        return request;
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

    private boolean atLeastOneLimitExist(Privilege privilege) {
        var limit = privilege.getLimit();
        return nonNull(limit.getDaily()) || nonNull(limit.getWeekly()) || nonNull(limit.getMonthly())
            || nonNull(limit.getQuarterly()) || nonNull(limit.getYearly()) || nonNull(limit.getTransactional());
    }

    private boolean limitsExist(BusinessFunctionGroup businessFunctionGroup) {
        return businessFunctionGroup.getFunctions()
            .stream()
            .flatMap(businessFunction -> businessFunction.getPrivileges().stream())
            .anyMatch(privilege -> nonNull(privilege.getLimit()));
    }

    private boolean noLimitsInJobRole(List<? extends JobRole> jobRoles) {
        return CollectionUtils.isEmpty(jobRoles) || jobRoles.stream()
            .filter(jobRole -> nonNull(jobRole.getFunctionGroups()))
            .flatMap(jobRole -> jobRole.getFunctionGroups().stream())
            .filter(businessFunctionGroup -> nonNull(businessFunctionGroup.getFunctions()))
            .flatMap(businessFunctionGroup -> businessFunctionGroup.getFunctions().stream())
            .filter(businessFunction -> nonNull(businessFunction.getPrivileges()))
            .flatMap(businessFunction -> businessFunction.getPrivileges().stream())
            .noneMatch(privilege -> nonNull(privilege.getLimit()));
    }

    private Mono<ServiceAgreementTaskV2> retrieveUsersInternalIdsForJobProfile(ServiceAgreementTaskV2 streamTask) {
        var sa = streamTask.getServiceAgreement();
        if(sa.getParticipants() == null || CollectionUtils.isEmpty(sa.getJobProfileUsers())) {
            return Mono.just(streamTask);
        }

        var jobProfileUsers = new HashSet<>(sa.getJobProfileUsers());
        return Flux.fromIterable(jobProfileUsers)
            .flatMap(jpu -> accessGroupService.getUserByExternalId(jpu.getUser().getExternalId(), true))
            .collectList()
            .flatMap(internalUsers -> {
                Map<String, GetUser> usersByExternalId =
                    internalUsers.stream().collect(Collectors.toMap(GetUser::getExternalId, Function.identity(), (a1, a2) -> a1));
                jobProfileUsers.forEach(jp -> {
                    String externalId = jp.getUser().getExternalId();
                    GetUser internalUser = usersByExternalId.get(externalId);
                    if (internalUser != null) {
                        jp.getUser().setInternalId(internalUser.getId());
                    }
                });
                return Mono.just(streamTask);
            });
    }
}
