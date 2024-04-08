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
import com.backbase.stream.exceptions.LegalEntityException;
import com.backbase.stream.legalentity.model.BusinessFunctionLimit;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.Limit;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.mapper.ExternalContactMapper;
import com.backbase.stream.mapper.LegalEntityV2toV1Mapper;
import com.backbase.stream.mapper.ServiceAgreementV2ToV1Mapper;
import com.backbase.stream.mapper.UserProfileMapper;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import io.micrometer.tracing.annotation.SpanTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
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
    public static final String USER = "USER";
    public static final String UPSERT_LEGAL_ENTITY = "upsert-legal-entity";
    public static final String FAILED = "failed";
    public static final String EXISTS = "exists";
    public static final String CREATED = "created";

    public static final String UPDATED = "updated";
    public static final String PROCESS_LIMITS = "process-limits";
    public static final String PROCESS_CONTACTS = "process-contacts";
    public static final String UPSERT = "upsert";

    private static final String LEGAL_ENTITY_E_TYPE = "LE";
    private static final String SERVICE_AGREEMENT_E_TYPE = "SA";
    private static final String FUNCTION_E_TYPE = "FUN";
    private static final String PRIVILEGE_E_TYPE = "PRV";
    private static final String LEGAL_ENTITY_LIMITS = "legal-entity-limits";

    private final UserProfileMapper userProfileMapper = Mappers.getMapper(UserProfileMapper.class);
    private final LegalEntityV2toV1Mapper leV2Mapper = Mappers.getMapper(LegalEntityV2toV1Mapper.class);
    private final ServiceAgreementV2ToV1Mapper saV2Mapper = Mappers.getMapper(ServiceAgreementV2ToV1Mapper.class);

    private final LegalEntityService legalEntityService;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final AccessGroupService accessGroupService;
    private final LimitsSaga limitsSaga;
    private final ContactsSaga contactsSaga;
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties;
    private final UserKindSegmentationSaga userKindSegmentationSaga;

    private static final ExternalContactMapper externalContactMapper = ExternalContactMapper.INSTANCE;

    public LegalEntitySagaV2(LegalEntityService legalEntityService,
        UserService userService,
        UserProfileService userProfileService,
        AccessGroupService accessGroupService,
        LimitsSaga limitsSaga,
        ContactsSaga contactsSaga,
        LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties,
        UserKindSegmentationSaga userKindSegmentationSaga) {
        this.legalEntityService = legalEntityService;
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.accessGroupService = accessGroupService;
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
        return switch (customerCategory) {
            case RETAIL -> UserKindEnum.RETAILCUSTOMER;
            case BUSINESS -> UserKindEnum.SME;
        };
    }

    private Mono<LegalEntityTaskV2> postLegalEntityContacts(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        if (isEmpty(legalEntity.getContacts())) {
            streamTask.info(LEGAL_ENTITY, PROCESS_CONTACTS, FAILED, legalEntity.getExternalId(),
                legalEntity.getInternalId(),
                "Legal Entity: %s does not have any Contacts defined", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }
        log.info("Creating Contacts for Legal Entity Id {}", legalEntity.getExternalId());
        Optional<String> externalUserOptional = Optional.empty();
        Optional<User> optionalUser =
            legalEntity.getUsers() != null ? legalEntity.getUsers().stream().findFirst() : Optional.empty();
        if (optionalUser.isPresent()) {
            externalUserOptional = Optional.of(optionalUser.get().getExternalId());
        }
        if (externalUserOptional.isEmpty()) {
            streamTask.info(LEGAL_ENTITY, PROCESS_CONTACTS, FAILED, legalEntity.getExternalId(),
                legalEntity.getInternalId(),
                "Legal Entity: %s does not have any Users", legalEntity.getExternalId());
            return Mono.just(streamTask);
        }
        return contactsSaga.executeTask(
                createContactsTask(streamTask.getId(), legalEntity.getExternalId(), null,
                    externalUserOptional.get(), AccessContextScope.LE, legalEntity.getContacts()))
            .flatMap(contactsTask -> requireNonNull(Mono.just(streamTask)))
            .then(Mono.just(streamTask));
    }

    private ContactsTask createContactsTask(String streamTaskId, String externalLegalEntityId,
        String externalServiceAgreementId, String externalUserId, AccessContextScope scope,
        List<ExternalContact> contacts) {
        var contactData = new ContactsBulkPostRequestBody();
        contactData.setIngestMode(IngestMode.UPSERT);
        contactData.setAccessContext(
            createExternalAccessContext(externalLegalEntityId, externalServiceAgreementId, externalUserId, scope));
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
                legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(legalEntityExternalId).map(saV2Mapper::mapV2),
                legalEntityService.getLegalEntityByExternalId(legalEntityExternalId).map(
                    leV2Mapper::mapLegalEntityToLegalEntityV2))
            .flatMap(data -> {
                LegalEntityV2 le = data.getT2();
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
                            REMOVED_PREFIX)).toList()))
                    .map(getUsers -> Tuples.of(data.getT1(), le, getUsers));
            })
            .flatMap(data -> {
                ServiceAgreementV2 sa = data.getT1();
                List<GetUser> users = data.getT3();
                return Flux.fromIterable(users)
                    .flatMap(user -> accessGroupService.removePermissionsForUser(sa.getInternalId(), user.getId())
                        .thenReturn(user.getExternalId()))
                    .collectList()
                    .map(userIds -> Tuples.of(sa, data.getT2(), userIds));

            })
            .flatMap(data -> {
                ServiceAgreementV2 sa = data.getT1();
                LegalEntityV2 le = data.getT2();
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
        task.info(LEGAL_ENTITY, UPSERT, "", task.getData().getExternalId(), null,
            "Upsert Legal Entity with External ID: %s", task.getData().getExternalId());
        LegalEntityV2 legalEntityV2 = task.getData();
        // Pipeline for Existing Legal Entity
        Mono<LegalEntityTaskV2> existingLegalEntity = legalEntityService.getLegalEntityByExternalId(
                legalEntityV2.getExternalId())
            .flatMap(actual -> {
                task.getData().setInternalId(actual.getInternalId());
                return legalEntityService.getLegalEntityByInternalId(actual.getInternalId())
                    .flatMap(result -> {
                        task.getData().setParentInternalId(result.getParentInternalId());

                        return legalEntityService.putLegalEntity(leV2Mapper.mapLegalEntityV2ToLegalEntity(task.getData())).flatMap(leUpdated -> {
                            log.info("Updated LegalEntity: {}", leUpdated.getName());
                            task.info(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, UPDATED, legalEntityV2.getExternalId(),
                                actual.getInternalId(), "Legal Entity: %s updated", legalEntityV2.getName());
                            return Mono.just(task);
                        });
                    });
            })
            .onErrorResume(throwable -> {
                if (throwable instanceof WebClientResponseException webClientResponseException) {
                    task.error(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, FAILED, legalEntityV2.getExternalId(), null,
                        webClientResponseException,
                        webClientResponseException.getResponseBodyAsString(), "Unexpected Web Client Exception");
                } else {
                    task.error(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, FAILED, legalEntityV2.getExternalId(), null, throwable,
                        throwable.getMessage(), "Unexpected Error");
                }
                return Mono.error(
                    new StreamTaskException(task, throwable, "Failed to get Legal Entity: " + throwable.getMessage()));
            });
        // Pipeline for Creating New Legal Entity
        Mono<LegalEntityTaskV2> createNewLegalEntity = Mono.defer(
            () -> legalEntityService.createLegalEntity(leV2Mapper.mapLegalEntityV2ToLegalEntity(legalEntityV2))
                .flatMap(actual -> {
                    task.getData().setInternalId(actual.getInternalId());
                    return legalEntityService.getLegalEntityByInternalId(actual.getInternalId())
                        .flatMap(result -> {
                            task.getData().setParentInternalId(result.getParentInternalId());
                            task.info(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, CREATED, legalEntityV2.getExternalId(),
                                actual.getInternalId(), "Created new Legal Entity");
                            return Mono.just(task);
                        });
                })
                .onErrorResume(LegalEntityException.class, legalEntityException -> {
                    task.error(LEGAL_ENTITY, UPSERT_LEGAL_ENTITY, FAILED, legalEntityV2.getExternalId(),
                        legalEntityV2.getInternalId(), legalEntityException, legalEntityException.getHttpResponse(),
                        legalEntityException.getMessage());
                    return Mono.error(new StreamTaskException(task, legalEntityException));
                }));
        return existingLegalEntity.switchIfEmpty(createNewLegalEntity);
    }

    private Mono<LegalEntityTaskV2> setupAdministrators(LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        Flux<User> administrators = Flux.fromStream(nullableCollectionToStream(legalEntity.getAdministrators()));

        return administrators.flatMap(user -> upsertUser(streamTask, user))
            .collectList()
            .map(legalEntity::administrators)
            .map(streamTask::data);
    }

    private Mono<LegalEntityTaskV2> setupUsers(LegalEntityTaskV2 streamTask) {
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

    public Mono<User> upsertUser(LegalEntityTaskV2 streamTask, User user) {
        if (legalEntitySagaConfigurationProperties.isUseIdentityIntegration()
            && !IdentityUserLinkStrategy.IDENTITY_AGNOSTIC.equals(user.getIdentityLinkStrategy())) {
            return upsertIdentityUser(streamTask, user);
        } else {
            log.debug(
                "Fallback to Identity Agnostic identityLinkStrategy. Either identity integration is disabled or User identityLinkStrategy is not set to identity.");
            return upsertUser(user, streamTask);
        }
    }

    private Mono<User> upsertUser(User user, LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        streamTask.info(USER, UPSERT, "", user.getExternalId(), "Upsert User with External ID: %s",
            user.getExternalId());

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

        Mono<User> createNewUser = Mono.zip(Mono.just(user),
            userService.createUser(user, legalEntity.getExternalId(), streamTask),
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
                streamTask.info(IDENTITY_USER, UPSERT, EXISTS, user.getExternalId(), user.getInternalId(),
                    "User %s already exists", existingUser.getExternalId());
                return user;
            })
            .flatMap(userService::updateIdentity);

        Mono<User> createNewIdentityUser =
            userService.createOrImportIdentityUser(user, legalEntity.getInternalId(), streamTask)
                .flatMap(currentUser -> userService.updateUserState(currentUser, legalEntity.getRealmName()))
                .map(existingUser -> {
                    user.setInternalId(existingUser.getInternalId());
                    streamTask.info(IDENTITY_USER, UPSERT, CREATED, user.getExternalId(), user.getInternalId(),
                        "User %s created", existingUser.getExternalId());
                    return user;
                });
        return getExistingIdentityUser.switchIfEmpty(createNewIdentityUser);
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
            .zipWith(Mono.just(streamTask), (legalEntityResults, childStreamTask) -> streamTask);
    }

    private Mono<LegalEntityTaskV2> linkLegalEntityToRealm(LegalEntityTaskV2 streamTask) {
        LegalEntity legalEntity = leV2Mapper.mapLegalEntityV2ToLegalEntity(streamTask.getLegalEntityV2());
        return Mono.just(streamTask)
            .filter(task -> legalEntitySagaConfigurationProperties.isUseIdentityIntegration())
            .flatMap(task ->
                userService.setupRealm(legalEntity)
                    .then(userService.linkLegalEntityToRealm(legalEntity))
                    .map(legalEntityV2 -> streamTask)
            ).switchIfEmpty(Mono.just(streamTask));
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
        if (isNull(legalEntity.getLimit()) || !validateLimit(legalEntity.getLimit())) {
            streamTask.info(LEGAL_ENTITY, PROCESS_LIMITS, FAILED, legalEntity.getInternalId(),
                legalEntity.getExternalId(), "Legal Entity: %s does not have any Legal Entity limits defined",
                legalEntity.getExternalId());
            return Mono.just(streamTask);
        }

        return limitsSaga.executeTask(
                createLimitsTask(streamTask, null, legalEntity.getInternalId(), legalEntity.getLimit()))
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
            streamTask.info(LEGAL_ENTITY, PROCESS_LIMITS, FAILED, legalEntity.getInternalId(),
                legalEntity.getExternalId(), "Legal Entity: %s does not have any Legal Entity limits defined",
                legalEntity.getExternalId());
            return Mono.just(streamTask);
        }
        return Flux.fromStream(legalEntity.getLimit().getBusinessFunctionLimits()
                .stream()
                .filter(businessFunctionLimit -> nonNull(businessFunctionLimit)
                    && !CollectionUtils.isEmpty(businessFunctionLimit.getPrivileges()))
                .flatMap(businessFunctionLimit -> createLimitsTask(streamTask, legalEntity.getInternalId(),
                    businessFunctionLimit)))
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

    private Stream<LimitsTask> createLimitsTask(LegalEntityTaskV2 streamTask, String legalEntityId,
        BusinessFunctionLimit businessFunctionLimit) {

        if (isNull(businessFunctionLimit) || CollectionUtils.isEmpty(businessFunctionLimit.getPrivileges())) {
            return Stream.of();
        }
        return businessFunctionLimit.getPrivileges()
            .stream()
            .filter(privilege -> validateLimit(privilege.getLimit()))
            .map(privilege -> {
                var limitData = new CreateLimitRequestBody();
                var entities = new ArrayList<Entity>();
                ofNullable(legalEntityId).ifPresent(
                    le -> entities.add(new Entity().etype(LEGAL_ENTITY_E_TYPE).eref(le)));
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

    private LimitsTask createLimitsTask(LegalEntityTaskV2 streamTask, ServiceAgreementV2 serviceAgreement,
        String legalEntityId, Limit limit) {

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
