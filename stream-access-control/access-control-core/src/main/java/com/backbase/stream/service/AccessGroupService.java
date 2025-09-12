package com.backbase.stream.service;

import static com.backbase.stream.legalentity.model.ServiceAgreementUserAction.ActionEnum.ADD;
import static com.backbase.stream.legalentity.model.ServiceAgreementUserAction.ActionEnum.REMOVE;

import com.backbase.accesscontrol.assignpermissions.api.service.v1.AssignPermissionsApi;
import com.backbase.accesscontrol.assignpermissions.api.service.v1.model.UserPermissionItem;
import com.backbase.accesscontrol.assignpermissions.api.service.v1.model.UserPermissions;
import com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action;
import com.backbase.accesscontrol.datagroup.api.integration.v1.model.DataGroupNameIdentifier;
import com.backbase.accesscontrol.datagroup.api.integration.v1.model.DataItemBatchUpdate;
import com.backbase.accesscontrol.datagroup.api.service.v1.DataGroupApi;
import com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup;
import com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroupCreateRequest;
import com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroupUpdateRequest;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.BatchResponseItemExtended.StatusEnum;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupBatchPutItem;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupNameIdentifier;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupUpdate;
import com.backbase.accesscontrol.functiongroup.api.service.v1.FunctionGroupApi;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupCreateRequest;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.GetFunctionGroups;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.Permission;
import com.backbase.accesscontrol.permissioncheck.api.service.v1.PermissionCheckApi;
import com.backbase.accesscontrol.permissioncheck.api.service.v1.model.UserDataItemPermissionsList;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementAdmin;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementAdminsBatchUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementUserExternal;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementUsersBatchUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.StatusCode;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.UpdateParticipantItem;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Admin;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.CustomerCategory;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Participant;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ParticipantWithAdminsAndUsers;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ResultId;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementParticipants;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Status;
import com.backbase.accesscontrol.usercontext.api.service.v1.UserContextApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.IdItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationDataGroupIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationIdentifier;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.configuration.AccessControlConfigurationProperties;
import com.backbase.stream.configuration.DeletionProperties;
import com.backbase.stream.configuration.DeletionProperties.FunctionGroupItemType;
import com.backbase.stream.legalentity.model.ApsIdentifiers;
import com.backbase.stream.legalentity.model.AssignedPermission;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup.TypeEnum;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementUserAction;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.mapper.AccessGroupMapper;
import com.backbase.stream.mapper.ParticipantMapper;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.utils.BatchResponseUtils;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Access Group Service provide access to Access Control, Data Groups and Function Groups from a single service.
 */
@Slf4j
@RequiredArgsConstructor
public class AccessGroupService {

    public static final String CREATE_ACCESS_GROUP = "create-access-group";
    private static final String SERVICE_AGREEMENT = "service-agreement";
    public static final String ACCESS_GROUP = "access-group";
    public static final String REJECTED = "rejected";
    public static final String CREATED = "created";
    public static final String FUNCTION_GROUP = "function-group";
    public static final String JOB_ROLE = "job-role";
    public static final String SETUP_FUNCTION_GROUP = "setup-function-group";
    public static final String SETUP_JOB_ROLE = "setup-job-role";
    private static final String FAILED = "failed";

    @NonNull
    private final UserManagementApi usersApi;
    @NonNull
    private final PermissionCheckApi permissionCheckServiceApi;
    @NonNull
    private final DataGroupApi dataGroupServiceApi;
    @NonNull
    private final com.backbase.accesscontrol.datagroup.api.integration.v1.DataGroupApi dataGroupIntegrationApi;
    @NonNull
    private final FunctionGroupApi functionGroupServiceApi;
    @NonNull
    private final com.backbase.accesscontrol.functiongroup.api.integration.v1.FunctionGroupApi functionGroupIntegrationApi;
    @NonNull
    private final ServiceAgreementApi serviceAgreementServiceApi;
    @NonNull
    private final com.backbase.accesscontrol.serviceagreement.api.integration.v1.ServiceAgreementApi serviceAgreementIntegrationApi;
    @NonNull
    private final AssignPermissionsApi assignPermissionsServiceApi;
    @NonNull
    private final com.backbase.accesscontrol.assignpermissions.api.integration.v1.AssignPermissionsApi assignPermissionsIntegrationApi;
    @NonNull
    private final DeletionProperties deletionProperties;
    @NonNull
    private final BatchResponseUtils batchResponseUtils;

    private final UserContextApi userContextApi;

    private final AccessControlConfigurationProperties accessControlProperties;

    private final AccessGroupMapper accessGroupMapper = Mappers.getMapper(AccessGroupMapper.class);

    private final ParticipantMapper participantMapper = Mappers.getMapper(ParticipantMapper.class);

    /**
     * Create Service Agreement.
     *
     * @param streamTask       The task invoking the call
     * @param serviceAgreement Service Agreement
     * @return Created Service Agreement
     */
    public Mono<ServiceAgreement> createServiceAgreement(StreamTask streamTask, ServiceAgreement serviceAgreement) {
        ServiceAgreementCreateRequest servicesAgreementIngest = map(serviceAgreement);
        return serviceAgreementServiceApi.createServiceAgreement(servicesAgreementIngest)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                streamTask.error(SERVICE_AGREEMENT, "create", "failed", serviceAgreement.getExternalId(),
                    "", throwable, throwable.getResponseBodyAsString(), "Failed to create Service Agreement");
                return Mono.error(new StreamTaskException(streamTask, throwable, "Failed to create Service Agreement"));
            })
            .zipWith(Mono.just(serviceAgreement), storeIdInServiceAgreement());
    }

    private ServiceAgreementCreateRequest map(ServiceAgreement serviceAgreement) {
        //TODO move to a mapper class
        return new ServiceAgreementCreateRequest().name(serviceAgreement.getName())
            .description(serviceAgreement.getDescription())
            .status(Status.valueOf(serviceAgreement.getStatus().toString()))
            .externalId(serviceAgreement.getExternalId())
//            .validFrom() //todo
//            .validUntil()
            .isSingle(serviceAgreement.getIsMaster())
            .creatorLegalEntity(serviceAgreement.getCreatorLegalEntity())
            .regularUserApsIds(mapAspIds(serviceAgreement.getRegularUserAps()))
            .adminUserApsIds(mapAspIds(serviceAgreement.getAdminUserAps()))
            .customerCategory(CustomerCategory.valueOf(serviceAgreement.getCustomerCategory().toString()))
            .purpose(serviceAgreement.getPurpose())
            .participants(mapParticipants(serviceAgreement.getParticipants()))
            .additions(serviceAgreement.getAdditions());
    }

    private List<ParticipantWithAdminsAndUsers> mapParticipants(List<LegalEntityParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }
        return participants.stream().map(participant ->
                new ParticipantWithAdminsAndUsers()
                    .legalEntityId(participant.getInternalId()) //todo check if defined
                    .sharingUsers(participant.getSharingUsers())
                    .sharingAccounts(participant.getSharingAccounts())
                    .admins(participant.getAdmins().stream().map(admin -> new Admin().userId(admin)).toList())
                    .users(participant.getUsers().stream()
                        .map(user -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(
                            user)).toList()))
            .toList();
    }

    private Set<Long> mapAspIds(ApsIdentifiers aspIdentifiers) {
        return aspIdentifiers == null ? Collections.emptySet() :
            aspIdentifiers.getIdIdentifiers().stream().map(BigDecimal::longValue).collect(Collectors.toSet());
    }

    /**
     * Get Service Agreement by external ID.
     *
     * @param externalId External Service Agreement ID
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> getServiceAgreementByExternalId(String externalId) {
        log.info("setting up getting Service Agreement with external Id: {} flow", externalId);
        return serviceAgreementIntegrationApi.getServiceAgreementByExternalId(externalId)
            .doOnNext(serviceAgreementItem -> log
                .info("Service Agreement: {} found", serviceAgreementItem.getExternalId()))
            .onErrorResume(WebClientResponseException.NotFound.class, throwable -> {
                log.info("Service Agreement with external Id {} not found. Request:[{}] {}  Response: {}",
                    externalId, throwable.getRequest().getMethod(), throwable.getRequest().getURI(),
                    throwable.getResponseBodyAsString());
                return Mono.empty();
            })
            .map(accessGroupMapper::toStream);
    }

    /**
     *
     * @param streamTask
     * @param serviceAgreement
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> updateServiceAgreementItem(StreamTask streamTask, ServiceAgreement serviceAgreement) {
        log.info("Updating Service Agreement with external Id: {}", serviceAgreement.getExternalId());
        ServiceAgreementUpdateRequest serviceAgreementPut = accessGroupMapper.toPresentationPut(serviceAgreement);
        return serviceAgreementServiceApi.putServiceAgreement(serviceAgreement.getInternalId(), serviceAgreementPut)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                log.error(SERVICE_AGREEMENT, "update", "failed", serviceAgreement.getExternalId(),
                    "", throwable, throwable.getResponseBodyAsString(), "Failed to update Service Agreement");
                return Mono.error(new StreamTaskException(streamTask, throwable, "Failed to update Service Agreement"));
            })
            .thenReturn(serviceAgreement);
    }

    /**
     * Update Service Agreement.
     *
     * @param streamTask          Stream task
     * @param serviceAgreement    Service agreement
     * @param regularUsersActions Service Agreement regular users actions
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> updateServiceAgreementAssociations(StreamTask streamTask,
        ServiceAgreement serviceAgreement,
        List<ServiceAgreementUserAction> regularUsersActions) {
        log.info("setting up Service Agreement with external Id: {}, associations update flow",
            serviceAgreement.getExternalId());

        Mono<Map<LegalEntityParticipant.ActionEnum, Mono<ServiceAgreement>>> updateParticipantsByActionMono =
            updateParticipants(streamTask, serviceAgreement);
        return updateParticipantsByActionMono.flatMap(updateParticipantsByAction ->
            updateParticipantsByAction.get(LegalEntityParticipant.ActionEnum.ADD)
                .then(updateServiceAgreementRegularUsers(streamTask, serviceAgreement, regularUsersActions))
                .then(updateParticipantsByAction.get(LegalEntityParticipant.ActionEnum.REMOVE))
        );
    }

    /**
     * Update regular users of service agreement.
     *
     * @param streamTask       Stream task
     * @param serviceAgreement Service agreement
     * @param actions          Service Agreement regular users actions
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> updateServiceAgreementRegularUsers(StreamTask streamTask,
        ServiceAgreement serviceAgreement,
        List<ServiceAgreementUserAction> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return Mono.just(serviceAgreement);
        }

        log.debug("setting up Service Agreement's regular users association flow.");

        return enrichUsersWithInternalUserId(streamTask, actions.stream()
            .map(ServiceAgreementUserAction::getUserProfile).map(JobProfileUser::getUser)
            .collect(Collectors.toList()))
            .flatMap(task -> getServiceAgreementUsers(serviceAgreement))
            .flatMapMany(existingUsers -> {

                Set<String> existingUsersSet = new HashSet<>(existingUsers);

                Predicate<ServiceAgreementUserAction> existing = ac ->
                    ac.getUserProfile().getUser().getInternalId() != null
                        && existingUsersSet.contains(ac.getUserProfile().getUser().getInternalId());

                Predicate<ServiceAgreementUserAction> notExistingToAdd = existing.negate()
                    .and(ac -> ac.getAction() == ADD);

                Predicate<ServiceAgreementUserAction> existingToRemove = existing
                    .and(ac -> ac.getAction() == REMOVE);

                List<ServiceAgreementUserAction> toAffect = actions.stream()
                    .filter(notExistingToAdd.or(existingToRemove))
                    .collect(Collectors.toList());

                List<ServiceAgreementUsersBatchUpdateRequest> actionsGroups =
                    buildServiceAgreementUserActionGroups(serviceAgreement, toAffect);

                log.debug("associating users to service agreement {}, request: {}", serviceAgreement.getExternalId(),
                    Arrays.toString(actionsGroups.toArray()));

                return Flux.fromIterable(actionsGroups);
            })
            .flatMap(actionGroup -> {
                log.info("Update regular users of Service Agreement with external Id: {}",
                    serviceAgreement.getExternalId());
                return serviceAgreementIntegrationApi.batchUpdateServiceAgreementUsers(actionGroup)
                    .onErrorResume(WebClientResponseException.class,
                        e -> Mono.error(new StreamTaskException(streamTask, e,
                            MessageFormat
                                .format("Failed to update user for Service Agreement with external id: {0}",
                                    serviceAgreement.getExternalId()))))
                    .collectList();
            })
            .collectList()
            .flatMap(lists -> Mono.just(lists.stream().flatMap(List::stream).collect(Collectors.toList())))
            .flatMap(list -> {
                list.stream().filter(r -> r.getStatus() != StatusCode.HTTP_STATUS_OK).forEach(r -> {
                    String errorMessage = "error associating user to Service Agreement" + r.toString();
                    log.error(errorMessage);
                    streamTask.error(SERVICE_AGREEMENT, "update-regular-users", "failed",
                        serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), errorMessage);
                    streamTask.setState(StreamTask.State.FAILED);
                });
                if (streamTask.isFailed()) {
                    return Mono.error(
                        new StreamTaskException(streamTask, "failed to associate regular users to Service Agreement"));
                }
                return Mono.just(serviceAgreement);
            });
    }

    private <T extends StreamTask> Mono<T> enrichUsersWithInternalUserId(T task, List<User> users) {
        List<User> usersMissingInternalId = users.stream().filter(u -> u.getInternalId() == null)
            .collect(Collectors.toList());
        return Flux.fromIterable(usersMissingInternalId)
            .flatMap(u -> getUserByExternalId(u.getExternalId(), true).doOnNext(gu -> u.setInternalId(gu.getId())))
            .collectList()
            .thenReturn(task);
    }

    private Mono<List<String>> getServiceAgreementUsers(ServiceAgreement serviceAgreement) {
        return fetchAllUsersPages(serviceAgreement.getInternalId(), null, 1000)
            .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                log.info("users not found");
                return Flux.just();
            })
            .collectList();
    }

    public Flux<String> fetchAllUsersPages(String serviceAgreementInternalId, String cursor, int size) {
        return serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementInternalId, cursor, size)
            .flatMapMany(response -> {
                Flux<String> currentPage = Flux.fromIterable(response.getUserIds().stream().map(
                    com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User::getUserId).toList());
                if (!response.getUserIds().isEmpty()) {
                    return Flux.concat(currentPage,
                        fetchAllUsersPages(serviceAgreementInternalId, response.getNextPage(), size));
                } else {
                    return currentPage;
                }
            });
    }

    @NotNull
    private List<ServiceAgreementUsersBatchUpdateRequest> buildServiceAgreementUserActionGroups(
        ServiceAgreement serviceAgreement, List<ServiceAgreementUserAction> actions) {
        return actions.stream()
            .filter(saUa -> saUa.getAction() != null)
            .collect(Collectors.groupingBy(ServiceAgreementUserAction::getAction))
            .entrySet().stream().map(actionGroup ->
                new ServiceAgreementUsersBatchUpdateRequest()
                    .action(com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Action.valueOf(
                        actionGroup.getKey().getValue()))
                    .users(actionGroup.getValue().stream().map(ServiceAgreementUserAction::getUserProfile)
                        .map(JobProfileUser::getUser).map(User::getExternalId)
                        .map(id -> new ServiceAgreementUserExternal()
                            .externalServiceAgreementId(serviceAgreement.getExternalId())
                            .externalUserId(id)).collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    /**
     * Update Service Agreement's participants.
     *
     * @param streamTask       Stream task
     * @param serviceAgreement Service agreement
     * @return Service Agreement
     */
    public Mono<Map<LegalEntityParticipant.ActionEnum, Mono<ServiceAgreement>>> updateParticipants(
        StreamTask streamTask, ServiceAgreement serviceAgreement) {

        Map<LegalEntityParticipant.ActionEnum, Mono<ServiceAgreement>> monoMap =
            new EnumMap<>(LegalEntityParticipant.ActionEnum.class);
        if (CollectionUtils.isEmpty(serviceAgreement.getParticipants())) {
            monoMap.put(LegalEntityParticipant.ActionEnum.REMOVE, Mono.just(serviceAgreement));
            monoMap.put(LegalEntityParticipant.ActionEnum.ADD, Mono.just(serviceAgreement));
            return Mono.just(monoMap);
        }
        serviceAgreement.getParticipants().forEach(p -> {
            if (p.getAction() == null) {
                p.setAction(LegalEntityParticipant.ActionEnum.ADD);
            }
        });

        return Mono.fromCallable(() -> {
                log.info("Updating participants of Service Agreement with external Id: {}",
                    serviceAgreement.getExternalId());
                return serviceAgreement;
            })
            .flatMap(sa -> getServiceAgreementParticipants(streamTask, serviceAgreement)
                .collectMap(Participant::getExternalId, Function.identity()))
            .flatMap(existingMap -> {

                log.debug("existing participants:" + Arrays.asList(existingMap.values().toArray()));

                List<LegalEntityParticipant> toRemove = serviceAgreement.getParticipants().stream()
                    .filter(p -> existingMap.get(p.getExternalId()) != null
                        && p.getAction() == LegalEntityParticipant.ActionEnum.REMOVE)
                    .toList();

                List<LegalEntityParticipant> toAdd = serviceAgreement.getParticipants().stream()
                    .filter(p -> existingMap.get(p.getExternalId()) == null
                        && p.getAction() == LegalEntityParticipant.ActionEnum.ADD)
                    .toList();

                List<UpdateParticipantItem> removeRequest = toRemove.stream()
                    .map(participant -> participantMapper.toPresentation(participant, serviceAgreement.getExternalId()))
                    .collect(Collectors.toList());

                List<UpdateParticipantItem> addRequest = toAdd.stream()
                    .map(participant -> participantMapper.toPresentation(participant, serviceAgreement.getExternalId()))
                    .collect(Collectors.toList());

                monoMap.put(LegalEntityParticipant.ActionEnum.ADD, putServiceAgreementParticipants(streamTask,
                    serviceAgreement, addRequest));
                monoMap.put(LegalEntityParticipant.ActionEnum.REMOVE, putServiceAgreementParticipants(streamTask,
                    serviceAgreement, removeRequest));

                return Mono.just(monoMap);
            });
    }

    public Flux<Participant> getServiceAgreementParticipants(StreamTask streamTask, ServiceAgreement serviceAgreement) {
        return serviceAgreementServiceApi.getServiceAgreementParticipants(serviceAgreement.getInternalId())
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
            .onErrorResume(WebClientResponseException.class, e -> {
                streamTask.error("participant", "update-participant", "failed",
                    serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), e, e.getMessage(),
                    "error retrieving participants for Service Agreement %s", serviceAgreement.getExternalId());
                return Mono.error(new StreamTaskException(streamTask, MessageFormat
                    .format("error retrieving participants for Service Agreement {0}",
                        serviceAgreement.getExternalId())));
            })
            .map(ServiceAgreementParticipants::getParticipants)
            .flatMapMany(Flux::fromIterable);
    }

    private Mono<ServiceAgreement> putServiceAgreementParticipants(StreamTask streamTask,
        ServiceAgreement serviceAgreement, List<UpdateParticipantItem> request) {
        if (CollectionUtils.isEmpty(request)) {
            return Mono.just(serviceAgreement);
        }

        log.debug("updating participants: " + request);

        return serviceAgreementIntegrationApi.batchUpdateParticipants(request)
            .onErrorResume(WebClientResponseException.class, e -> {
                streamTask.error("participant", "update-participants", "failed",
                    serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), e, e.getResponseBodyAsString(),
                    "Failed to update participants");
                return Mono.error(new StreamTaskException(streamTask, e,
                    MessageFormat
                        .format("Failed to update participants for Service Agreement with external id: {0}",
                            serviceAgreement.getExternalId())));
            })
            .collectList()
            .flatMap(resultList -> {
                resultList.stream().forEach(r -> {
                    if (r.getStatus() != StatusCode.HTTP_STATUS_OK) {
                        streamTask.error("participant", "update-participant", "failed", r.getResourceId(),
                            null, "Error updating Participant %s for Service Agreement: %s", r.getResourceId(),
                            serviceAgreement.getExternalId());
                        log.error("Error updating Participant {} for Service Agreement: {}", r.getResourceId(),
                            serviceAgreement.getExternalId());
                        streamTask.setState(StreamTask.State.FAILED);
                    }
                });
                if (streamTask.isFailed()) {
                    return Mono.error(new StreamTaskException(streamTask, "Failed to update participant"));
                }
                return Mono.just(serviceAgreement);
            });
    }

    /**
     * Retrieve user by external id.
     *
     * @param externalId         user external id
     * @param skipHierarchyCheck skip hierarchy check
     * @return User
     */
    public Mono<GetUser> getUserByExternalId(String externalId, boolean skipHierarchyCheck) {
        return usersApi.getUserByExternalId(externalId, skipHierarchyCheck)
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    private BiFunction<ResultId, ServiceAgreement, ServiceAgreement> storeIdInServiceAgreement() {
        return (idItem, serviceAgreement1) -> {
            serviceAgreement1.setInternalId(idItem.getId());
            log.info("Created Service Agreement: {} with id: {}", serviceAgreement1.getName(), idItem.getId());
            return serviceAgreement1;
        };
    }

    private BiFunction<IdItem, ServiceAgreementV2, ServiceAgreementV2> storeIdInServiceAgreementV2() {
        return (idItem, serviceAgreement1) -> {
            serviceAgreement1.setInternalId(idItem.getId());
            log.info("Created Service Agreement: {} with id: {}", serviceAgreement1.getName(), idItem.getId());
            return serviceAgreement1;
        };
    }

    public Mono<LegalEntity> setAdministrators(LegalEntity legalEntity) {

        List<ServiceAgreementAdmin> userPairs = legalEntity.getAdministrators().stream().map(user -> {
            ServiceAgreementAdmin usersItem = new ServiceAgreementAdmin();
            usersItem.setExternalServiceAgreementId(legalEntity.getMasterServiceAgreement().getExternalId());
            usersItem.setExternalUserId(user.getExternalId());
            return usersItem;
        }).collect(Collectors.toList());

        ServiceAgreementAdminsBatchUpdateRequest presentationServiceAgreementUsersBatchUpdate = new ServiceAgreementAdminsBatchUpdateRequest();
        presentationServiceAgreementUsersBatchUpdate.users(userPairs);
        presentationServiceAgreementUsersBatchUpdate.setAction(
            com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Action.ADD);

        return serviceAgreementIntegrationApi.batchUpdateServiceAgreementAdmins(
                presentationServiceAgreementUsersBatchUpdate)
            .doOnError(WebClientResponseException.BadRequest.class, this::handleError)
            .collectList()
            .map(batchResponseItemExtendeds -> {
                log.info("Setup Bank Admins: {}", batchResponseItemExtendeds);
                return legalEntity;
            });
    }

    /**
     * Assign permissions between user, business function groups and product groups for a Service Agreement.
     *
     * @param streamTask     Stream Task
     * @param jobProfileUser The user and business functions to assign permissions for
     * @return An approval status if approvals is enabled or an empty mojo on completion.
     */
    public Mono<JobProfileUser> assignPermissions(ProductGroupTask streamTask, JobProfileUser jobProfileUser) {
        streamTask.info(ACCESS_GROUP, "assign-product-permissions", "", streamTask.getProductGroup().getName(), null,
            "Assigning permissions for Data Group for: %s and Service Agreement: %s", streamTask.getData().getName(),
            streamTask.getData().getServiceAgreement().getExternalId());

        ProductGroup productGroup = streamTask.getData();
        ServiceAgreement serviceAgreement = productGroup.getServiceAgreement();

        List<UserPermissionItem> functionGroups = functionGroupsWithDataGroup(jobProfileUser, productGroup);

        return assignPermissions(streamTask, serviceAgreement, jobProfileUser, functionGroups);
    }

    /**
     * Assign permissions between user, business function groups for a Service Agreement.
     *
     * @param streamTask     Stream Task
     * @param jobProfileUser The user and business functions to assign permissions for
     * @return An approval status if approvals is enabled or an empty mojo on completion.
     */
    public Mono<JobProfileUser> assignPermissions(StreamTask streamTask, ServiceAgreement serviceAgreement,
        JobProfileUser jobProfileUser, List<UserPermissionItem> functionGroups) {

        streamTask.info(ACCESS_GROUP, "assign-permissions", "",
            streamTask.getName(), null,
            "Assigning permissions for Service Agreement: %s", serviceAgreement.getExternalId());

        String serviceAgreementId = serviceAgreement.getInternalId();
        String userId = jobProfileUser.getUser().getInternalId();

        return assignPermissionsServiceApi.putUserPermissions(serviceAgreementId, userId, functionGroups)
            .onErrorResume(WebClientResponseException.class, e -> {
                streamTask.error(ACCESS_GROUP, "assign-permissions", "failed", serviceAgreement.getExternalId(),
                    serviceAgreementId, e, e.getResponseBodyAsString(), "Failed to to assign permissions");
                return Mono.error(new StreamTaskException(streamTask, e,
                    "Failed to Assign permissions: " + e.getResponseBodyAsString()));
            })
            .then(Mono.just(jobProfileUser));
    }

    public Mono<BatchProductGroupTask> assignPermissionsBatch(BatchProductGroupTask task,
        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions) {

        List<UserWithPermissions> request = usersPermissions.keySet().stream()
            .map(user -> new UserWithPermissions(
                user, task.getData().getServiceAgreement(),
                usersPermissions.get(user).keySet().stream()
                    .map(bfg -> new UserPermissionItem()
                        .functionGroupId(bfg.getId())
                        .dataGroupIds(usersPermissions.get(user).get(bfg).stream()
                            .map(BaseProductGroup::getInternalId)
                            .collect(Collectors.toSet())))
                    .toList()))
            .collect(Collectors.toList());

        return Mono.just(request)
            .flatMap(userPermissionsRequest -> {
                if (task.getIngestionMode().isFunctionGroupsReplaceEnabled()) {
                    task.info(ACCESS_GROUP, "assign-permissions", "", "", null,
                        "Replacing assigned permissions for users: %s with: %s",
                        prettyPrintExternalIds(userPermissionsRequest),
                        prettyPrintDataGroups(userPermissionsRequest));
                    return Mono.just(userPermissionsRequest);
                }

                return getAssociatedSystemFunctionsIds(task)
                    .flatMap(systemFunctionGroupIds -> mergeUserPermissions(task, request, systemFunctionGroupIds))
                    .map(userPermissionsList -> {
                        log.info("Updated assigned permissions for users: {} with: {}",
                            prettyPrintExternalIds(userPermissionsList), prettyPrintDataGroups(userPermissionsList));
                        return userPermissionsList;
                    });
            })
            .flatMap(userPermissionsList -> {
                task.info(ACCESS_GROUP, "assign-permissions", task.getName(), null, task.getId(),
                    "Assigning permissions: %s",
                    userPermissionsList.stream().map(this::prettyPrintUserAssignedPermissions)
                        .collect(Collectors.joining(",")));
                return Flux.fromIterable(userPermissionsList)
                    .flatMap(permissions -> assignPermissionsServiceApi.putUserPermissions(
                        permissions.user().getInternalId(), permissions.serviceAgreement.getInternalId(),
                        permissions.permissionItems()))
                    .onErrorResume(WebClientResponseException.class, e -> {
                        task.error(ACCESS_GROUP, "assign-permissions", "failed",
                            task.getData().getServiceAgreement().getExternalId(),
                            task.getData().getServiceAgreement().getInternalId(), e, e.getResponseBodyAsString(),
                            "Failed to execute Batch Permissions assignment request.");
                        return Mono.error(new StreamTaskException(task, e,
                            "Failed  to assign permissions: " + e.getResponseBodyAsString()));
                    })
                    .collectList();
            })
            .thenReturn(task);
    }

    /**
     * Retrieves function groups by service agreement id, filter any non-system and convert resulting list into a set of
     * ids.
     */
    private Mono<Set<String>> getAssociatedSystemFunctionsIds(BatchProductGroupTask task) {
        return functionGroupServiceApi.getFunctionGroups(task.getData().getServiceAgreement().getInternalId())
            .onErrorResume(WebClientResponseException.class, e -> {
                task.error(ACCESS_GROUP, "assign-permissions", "failed",
                    task.getData().getServiceAgreement().getExternalId(),
                    task.getData().getServiceAgreement().getInternalId(), e, e.getResponseBodyAsString(),
                    "Failed to fetch function groups");
                return Mono.error(new StreamTaskException(task, e,
                    "Failed to fetch function groups: " + e.getResponseBodyAsString()));
            })
            .map(GetFunctionGroups::getFunctionGroups)
            .switchIfEmpty(Mono.just(Collections.emptyList()))
            .flatMapIterable(Function.identity())
            .filter(functionGroup -> FunctionGroupItem.TypeEnum.SYSTEM.equals(functionGroup.getType()))
            .map(FunctionGroupItem::getId)
            .collect(Collectors.toSet());
    }

    /**
     * Request each user permissions and add those to the request.
     *
     * @param task                   - Current task
     * @param request                - Input user permissions list
     * @param systemFunctionGroupIds - A set of system function group ids that belong to service agreement
     */
    private Mono<List<UserWithPermissions>> mergeUserPermissions(BatchProductGroupTask task,
        List<UserWithPermissions> request, Set<String> systemFunctionGroupIds) {
        return Flux.fromIterable(request)
            .flatMap(userPermissionItem -> assignPermissionsServiceApi.getUserPermissions(
                    userPermissionItem.user().getInternalId(),
                    task.getData().getServiceAgreement().getInternalId())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .map(UserPermissions::getPermissions)
                .map(items -> items.stream()
                    // System Function Group should not be part of PUT permissions request as it cannot be modified
                    .filter(
                        existingPermission -> !systemFunctionGroupIds.contains(existingPermission.getFunctionGroupId()))
                    .collect(Collectors.toList())
                )
                .map(existingUserPermissions -> {
                    log.info("Retrieved permissions for user with externalId {} : {}",
                        userPermissionItem.user().getExternalId(),
                        existingUserPermissions.stream()
                            .map(p -> p.getFunctionGroupId() + " : [" + p.getDataGroupIds() + "] ")
                            .collect(Collectors.toList()));

                    if (!existingUserPermissions.isEmpty()) {
                        existingUserPermissions.forEach(existingUserPermission -> {

                            Optional<UserPermissionItem> requestFunctionGroupOpt = userPermissionItem.permissionItems()
                                .stream()
                                .filter(existingPermissionItem -> existingPermissionItem.getFunctionGroupId()
                                    .equals(existingUserPermission.getFunctionGroupId()))
                                .findFirst();

                            if (requestFunctionGroupOpt.isPresent()) {
                                requestFunctionGroupOpt.get().getDataGroupIds()
                                    .addAll(existingUserPermission.getDataGroupIds());
                            } else {
                                userPermissionItem.permissionItems().add(existingUserPermission);
                            }
                        });
                    }
                    return userPermissionItem;
                }))
            .collectList();
    }

    private String prettyPrint(UserPermissionItem functionGroup) {
        return " functionGroup: " + functionGroup.getFunctionGroupId() +
            " dataGroupIds: " + prettyPrintPresentationDataGroups(functionGroup.getDataGroupIds());
    }

    private String prettyPrintUserAssignedPermissions(
        UserWithPermissions userPermissionItems) {
        return "User: " + userPermissionItems.user().getExternalId() +
            " Service Agreement: " + userPermissionItems.serviceAgreement().getExternalId() +
            " Function & Data Groups: " + userPermissionItems.permissionItems().stream()
            .map(this::prettyPrint).collect(Collectors.joining(", "));

    }


    private String prettyPrintPresentationDataGroups(Collection<String> dataGroupIdentifiers) {
        if (CollectionUtils.isEmpty(dataGroupIdentifiers)) {
            return "NO DATA GROUP IDS!";
        }
        return String.join(",", dataGroupIdentifiers);
    }

    private PresentationIdentifier mapFunctionGroup(String id) {
        return new PresentationIdentifier().idIdentifier(id);
    }

    private PresentationDataGroupIdentifier mapDataGroupId(String id) {
        return new PresentationDataGroupIdentifier().idIdentifier(id);
    }

    private String prettyPrintDataGroups(Collection<UserWithPermissions> userPermissionItems) {
        return userPermissionItems.stream().map(UserWithPermissions::permissionItems)
            .flatMap(Collection::stream)
            .map(item -> "functionGroupIdentifier: " + item.getFunctionGroupId()
                + " dataGroupItems: [" + String.join(",", item.getDataGroupIds()) + "]")
            .collect(Collectors.joining(", "));
    }

    private String prettyPrintExternalIds(Collection<UserWithPermissions> r) {
        return r.stream().map(UserWithPermissions::user).map(User::getExternalId).collect(Collectors.joining(", "));
    }

    private List<UserPermissionItem> functionGroupsWithDataGroup(JobProfileUser jobProfileUser,
        ProductGroup productGroup) {
        /// Must also include existing function groupss!!

        return jobProfileUser.getBusinessFunctionGroups().stream()
            .map(businessFunctionGroup -> new UserPermissionItem()
                .functionGroupId(businessFunctionGroup.getId())
                .dataGroupIds(Set.of(productGroup.getInternalId())))
            .collect(Collectors.toList());
    }

    /**
     * Link all products in Product Group with Service Agreement.
     *
     * @param streamTask Stream Task containing Product Group to link to Service Agreement
     * @return Product Group
     */
    public Mono<ProductGroupTask> setupProductGroups(ProductGroupTask streamTask) {
        streamTask.info(ACCESS_GROUP, "setup-product-group", "", streamTask.getProductGroup().getName(), null,
            "Setting up Data Group for: %s and Service Agreement: %s", streamTask.getData().getName(),
            streamTask.getData().getServiceAgreement().getExternalId());
        ProductGroup productGroup = streamTask.getData();
        ServiceAgreement serviceAgreement = productGroup.getServiceAgreement();

        return getExistingDataGroups(serviceAgreement.getInternalId(), productGroup.getProductGroupType().name())
            .doOnNext(dataGroupItem -> streamTask.info(ACCESS_GROUP, "setup-product-group", "exists",
                productGroup.getProductGroupType().name(), dataGroupItem.getId(), "Data Group already exists"))
            .onErrorResume(WebClientResponseException.BadRequest.class, e -> {
                streamTask.error(ACCESS_GROUP, "setup-product-group", "failed", serviceAgreement.getExternalId(),
                    serviceAgreement.getInternalId(),
                    "Failed to setup Access Group groups for Service Agreement: " + serviceAgreement.getInternalId());
                return Mono.error(new StreamTaskException(streamTask, e,
                    "Failed to setup Access Group for Service Agreement: " + serviceAgreement.getInternalId()));

            })
//            .filter(item -> isEquals(productGroup, item))
            .collectList()
            .doOnNext(dataGroupItem -> log.debug("Dataground found: {}", dataGroupItem))
            .flatMap(existingAccessGroups -> {
                Optional<DataGroup> first = existingAccessGroups.stream()
                    .filter(dataGroupItem -> dataGroupItem.getName().equals(productGroup.getName())).findFirst();
                return first.map(
                        dataGroupItem -> updateAccessGroupWithArrangementIds(dataGroupItem, productGroup, streamTask))
                    .orElseGet(() -> createArrangementDataAccessGroup(serviceAgreement, streamTask.getProductGroup(),
                        streamTask).cast(ProductGroup.class));
            }).map(streamTask::data);
    }

    public Mono<BatchProductGroupTask> updateExistingDataGroupsBatch(BatchProductGroupTask task,
        List<DataGroup> existingDataGroups, List<BaseProductGroup> productGroups) {
        List<DataItemBatchUpdate> batchUpdateRequest = new ArrayList<>();
        final Set<String> affectedArrangements = Stream.concat(
                productGroups.stream().map(StreamUtils::getInternalProductIds).flatMap(List::stream),
                productGroups.stream().map(BaseProductGroup::getCustomDataGroupItems).flatMap(List::stream)
                    .map(CustomDataGroupItem::getInternalId))
            .collect(Collectors.toSet());
        if (task.getIngestionMode().isDataGroupsReplaceEnabled()) {
            // if REPLACE mode, existing products (not sent in the request) also need to be added to the set of affected arrangements.
            affectedArrangements.addAll(existingDataGroups.stream()
                .map(DataGroup::getItems)
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
            );
        }

        existingDataGroups.forEach(dbsDataGroup -> {
            // get group matching  DBS one.
            Optional<BaseProductGroup> pg = productGroups.stream()
                .filter(it -> isEquals(it, dbsDataGroup))
                .findFirst();
            Set<String> arrangementsToAdd = new HashSet<>();
            Set<String> arrangementsToRemove = new HashSet<>();
            affectedArrangements.forEach(arrangement -> pg.ifPresent(p -> {
                boolean shouldBeInGroup = StreamUtils.getInternalProductIds(pg.get()).contains(arrangement) ||
                    pg.get().getCustomDataGroupItems().stream().map(CustomDataGroupItem::getInternalId)
                        .anyMatch(arrangement::equals);
                if (!dbsDataGroup.getItems().contains(arrangement) && shouldBeInGroup) {
                    // ADD.
                    log.debug("Arrangement item {} to be added to Data Group {}", arrangement, dbsDataGroup.getName());
                    arrangementsToAdd.add(arrangement);
                }
                if (dbsDataGroup.getItems().contains(arrangement) && !shouldBeInGroup) {
                    // remove.
                    log.debug("Arrangement item {} to be removed from Data Group {}", arrangement,
                        dbsDataGroup.getName());
                    arrangementsToRemove.add(arrangement);
                }
            }));
            if (!CollectionUtils.isEmpty(arrangementsToAdd)) {
                batchUpdateRequest.add(new DataItemBatchUpdate()
                    .dataGroupIdentifier(new DataGroupNameIdentifier()
                        .name(dbsDataGroup.getName())
                        .dataGroupType(dbsDataGroup.getType())
                        .externalServiceAgreementId(task.getData().getServiceAgreement().getExternalId()))
                    .action(Action.ADD)
                    .dataItems(arrangementsToAdd)
                );
            }
            if (!CollectionUtils.isEmpty(arrangementsToRemove)) {
                batchUpdateRequest.add(new DataItemBatchUpdate()
                    .dataGroupIdentifier(new DataGroupNameIdentifier()
                        .name(dbsDataGroup.getName())
                        .dataGroupType(dbsDataGroup.getType())
                        .externalServiceAgreementId(task.getData().getServiceAgreement().getExternalId()))
                    .action(Action.REMOVE)
                    .dataItems(arrangementsToRemove)
                );
            }
        });
        if (!CollectionUtils.isEmpty(batchUpdateRequest)) {
            return updateDataGroupItems(batchUpdateRequest)
                .doOnNext(response ->
                    task.info(ACCESS_GROUP, "update", response.getStatus().toString(), response.getResourceId(), null,
                        "Product group updated.")
                )
                .onErrorResume(WebClientResponseException.class, e -> {
                    task.error(ACCESS_GROUP, "product-group", "failed",
                        task.getData().getServiceAgreement().getExternalId(),
                        task.getData().getServiceAgreement().getInternalId(), e, e.getResponseBodyAsString(),
                        "Failed to update product groups");
                    return Mono.error(new StreamTaskException(task, e,
                        "Failed Update Product groups: " + e.getResponseBodyAsString()));
                })
                .collectList()
                .thenReturn(task);
        } else {
            log.debug("All Product Groups are up to date.");
            task.info(ACCESS_GROUP, "update", "SUCCESS", prettyPrintProductGroupNames(task), null,
                "All Product Groups are up to date.");
            return Mono.just(task);
        }
    }

    @NotNull
    private String prettyPrintProductGroupNames(BatchProductGroupTask task) {
        return task.getBatchProductGroup().getProductGroups().stream().map(BaseProductGroup::getName)
            .collect(Collectors.joining(","));
    }

    public Flux<com.backbase.accesscontrol.datagroup.api.integration.v1.model.BatchResponseItemExtended> updateDataGroupItems(
        List<DataItemBatchUpdate> request) {
        return dataGroupIntegrationApi.batchUpdateDataItems(request)
            .map(r -> batchResponseUtils.checkBatchResponseItem(r, "Product Groups Update", r.getStatus().toString(),
                r.getResourceId(), r.getErrors()));
    }

    public Flux<DataGroup> getExistingDataGroups(String serviceAgreementInternalId, String type) {
        return fetchAllDataGroupPages(serviceAgreementInternalId, type, null);
    }

    public Flux<DataGroup> fetchAllDataGroupPages(String serviceAgreementInternalId, String type, String cursor) {
        return dataGroupServiceApi.getDataGroups(serviceAgreementInternalId, type, true, cursor, 1000)
            .flatMapMany(response -> {
                Flux<DataGroup> currentPage = Flux.fromIterable(response.getDataGroups());
                if (!response.getDataGroups().isEmpty()) {
                    return Flux.concat(currentPage,
                        fetchAllDataGroupPages(serviceAgreementInternalId, type, response.getNextPage()));
                } else {
                    return currentPage;
                }
            });
    }

    /**
     * Setup Business Function Groups and link them to the Service Agreement.
     *
     * @param streamTask
     * @param serviceAgreement       Service Agreement
     * @param businessFunctionGroups Business function groups to connect to service agreement.
     * @return Job Profile User with updated Function Groups
     */
    public Mono<List<BusinessFunctionGroup>> setupFunctionGroups(StreamTask streamTask,
        ServiceAgreement serviceAgreement, List<BusinessFunctionGroup> businessFunctionGroups) {

        streamTask.info(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "", serviceAgreement.getExternalId(),
            serviceAgreement.getInternalId(), "Setting up %s Business Functions for Service Agreement: %s",
            businessFunctionGroups.size(), serviceAgreement.getName());
        log.info("Setup {} Business Function for Service Agreement: {}", businessFunctionGroups.size(),
            serviceAgreement.getExternalId());
        return getFunctionGroups(streamTask, serviceAgreement)
            .collectList()
            .flatMap(functionGroups -> {
                    log.debug("functionGroups: {}", functionGroups);
                    List<BusinessFunctionGroup> newBusinessGroups =
                        getNewBusinessGroups(businessFunctionGroups, functionGroups);
                    return Flux.fromIterable(newBusinessGroups)
                        .flatMap(bfg -> createBusinessFunctionGroup(streamTask, serviceAgreement, bfg))
                        .collectList()
                        .flatMap(bfg -> {
                            List<BusinessFunctionGroup> existingBusinessGroups =
                                getExistingBusinessGroups(businessFunctionGroups, functionGroups);
                            log.debug("existingBusinessGroups: {}", existingBusinessGroups);
                            return updateBatchBusinessFunctionGroup(streamTask, serviceAgreement, existingBusinessGroups)
                                .flatMap(updated -> {
                                    bfg.addAll(existingBusinessGroups);
                                    return Mono.just(bfg);
                                });
                        });
                }
            );
    }

    public boolean isEquals(BaseProductGroup productGroup, DataGroup item) {
        return item.getName().equals(productGroup.getName());
    }

    private boolean productGroupAndDataGrouItemEquals(BusinessFunctionGroup businessFunctionGroup,
        FunctionGroupItem getFunctionGroupsFunctionGroupItem) {
        return getFunctionGroupsFunctionGroupItem.getName().equals(businessFunctionGroup.getName());
    }


    private Mono<ProductGroup> updateAccessGroupWithArrangementIds(DataGroup dataGroup,
        ProductGroup productGroup, StreamTask streamTask) {

        streamTask.info(ACCESS_GROUP, "update-access-group", "", dataGroup.getName(),
            dataGroup.getId(), "Updating Data Group");

        log.info("Updating Data Access Group: {}", dataGroup.getId());

        Set<String> dataItems = Stream.concat(
                StreamUtils.getInternalProductIds(productGroup).stream(),
                StreamUtils.getCustomDataGroupItems(productGroup).stream())
            .collect(Collectors.toSet());

        DataGroupUpdateRequest dataGroupUpdateRequest = new DataGroupUpdateRequest();
        dataGroupUpdateRequest.setDescription(dataGroup.getDescription());
        dataGroupUpdateRequest.setName(dataGroup.getName());
        dataGroupUpdateRequest.setType(dataGroup.getType());
        dataGroupUpdateRequest.setDataItems(dataItems);
        dataGroupUpdateRequest.setAdditions(dataGroup.getAdditions());
        dataGroupUpdateRequest.setMetadata(dataGroup.getMetadata());

        return dataGroupServiceApi.updateDataGroup(dataGroup.getId(), dataGroupUpdateRequest)
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(ACCESS_GROUP, "update-access-group", "failed", productGroup.getName(),
                    dataGroup.getId(), badRequest, badRequest.getResponseBodyAsString(),
                    "Failed to update access group");
                log.error("Error Updating data access group: {}", badRequest.getResponseBodyAsString());
                return Mono.error(
                    new StreamTaskException(streamTask, badRequest, "Failed to update Data Access Group"));
            })
            .thenReturn(productGroup)
            .map(pg -> {
                streamTask.info(ACCESS_GROUP, "update-access-group", "updated", pg.getName(),
                    dataGroup.getId(),
                    "Updated Data Access Group: %s", pg.getName());
                log.info("Updated Data Access Group: {}", pg.getName());
                pg.setInternalId(dataGroup.getId());
                return pg;
            });
    }


    public Mono<BaseProductGroup> createArrangementDataAccessGroup(ServiceAgreement serviceAgreement,
        BaseProductGroup productGroup, StreamTask streamTask) {
        streamTask.info(ACCESS_GROUP, CREATE_ACCESS_GROUP, "", productGroup.getName(), null, "Create new Data Group");

        log.info("Creating Data Access Group: {}", productGroup.getName());

        Set<String> dataItems = Stream.concat(StreamUtils.getInternalProductIds(productGroup).stream(),
                StreamUtils.getCustomDataGroupItems(productGroup).stream())
            .collect(Collectors.toSet());
        log.info("Data Access Group data items: {}", dataItems);
        DataGroupCreateRequest dataGroupCreateRequest = new DataGroupCreateRequest();
        dataGroupCreateRequest.setName(productGroup.getName());
        dataGroupCreateRequest.setDescription(productGroup.getDescription());
        dataGroupCreateRequest.setServiceAgreementId(serviceAgreement.getInternalId());
        dataGroupCreateRequest.setItems(dataItems);
        dataGroupCreateRequest.setType(productGroup.getProductGroupType().name());
        if (dataGroupCreateRequest.getItems().stream().anyMatch(Objects::isNull)) {
            streamTask.error(ACCESS_GROUP, CREATE_ACCESS_GROUP, REJECTED, productGroup.getName(), null,
                "Data group items cannot have null items");
            throw new StreamTaskException(streamTask, "Data Group Items cannot have null items");
        }

        return dataGroupServiceApi.createDataGroup(dataGroupCreateRequest)
            .onErrorResume(WebClientResponseException.class, webClientResponseEx -> {
                String message = webClientResponseEx.getResponseBodyAsString();
                streamTask.error(ACCESS_GROUP, CREATE_ACCESS_GROUP, REJECTED, productGroup.getName(), null,
                    webClientResponseEx, message, "Unable to add data access group items");
                return Mono.error(new StreamTaskException(streamTask, webClientResponseEx, message));
            })
            .map(idItem -> {
                    streamTask.info(ACCESS_GROUP, CREATE_ACCESS_GROUP, CREATED, productGroup.getName(), idItem.getId(),
                        "Create new Data Group");
                    productGroup.setInternalId(idItem.getId());
                    return productGroup;
                }
            );

    }

    /**
     * Remove all permissions in service agreement  for specified user.
     *
     * @param serviceAgreementInternalId internal id of Service Agreement.
     * @param userInternalId             internal id of user.
     * @return Mono<Void>
     */
    public Mono<Void> removePermissionsForUser(String serviceAgreementInternalId, String userInternalId) {
        log.debug("Removing permissions from all Data Groups for user: {} in service agreement {}", userInternalId,
            serviceAgreementInternalId);
        return assignPermissionsServiceApi.putUserPermissions(
                serviceAgreementInternalId,
                userInternalId,
                List.of())
            .then();
    }

    /**
     * Delete all Function Groups  defined in  service agreement.
     *
     * @param serviceAgreementInternalId Sevice Agreement internal idenetifier.
     * @param serviceAgreementExternalId
     * @return Mono<Void>
     */
    public Mono<Void> deleteFunctionGroupsForServiceAgreement(String serviceAgreementInternalId,
        String serviceAgreementExternalId) {
        log.debug("Retrieving Function Groups for Service Agreement {}", serviceAgreementInternalId);
        if (deletionProperties.getFunctionGroupItemType().equals(FunctionGroupItemType.NONE)) {
            log.info("Skipped deleting Function Groups due to deletion configuration set to NONE");
            return Mono.empty();
        }

        return functionGroupServiceApi.getFunctionGroups(serviceAgreementInternalId)
            .map(GetFunctionGroups::getFunctionGroups)
            .switchIfEmpty(Mono.just(Collections.emptyList()))
            .flatMapIterable(Function.identity())
            .filter(functionGroup -> FunctionGroupItem.TypeEnum.REFERENCE.equals(functionGroup.getType()))
            .map(FunctionGroupItem::getName)
            .map(name -> new FunctionGroupNameIdentifier().name(name)
                .serviceAgreementExternalId(serviceAgreementExternalId))
            .collectList()
            .map(functionGroupIntegrationApi::batchDeleteFunctionGroups)
            .flatMapMany(f -> f.map(r -> batchResponseUtils.checkBatchResponseItem(r, "Function  Group Removal",
                    r.getStatus().getValue(), r.getResourceId(), r.getErrors()))
                .collectList())
            .then();
    }

    /**
     * Retrieve all Reference Job Roles defined in  service agreement.
     *
     * @param serviceAgreementInternalId Sevice Agreement internal idenetifier.
     * @return Mono<List < GetFunctionGroupsFunctionGroupItem>>
     */
    public Mono<List<FunctionGroupItem>> getFunctionGroupsForServiceAgreement(String serviceAgreementInternalId) {
        log.debug("Retrieving Function Groups for Service Agreement {}", serviceAgreementInternalId);
        return functionGroupServiceApi.getFunctionGroups(serviceAgreementInternalId)
            .map(GetFunctionGroups::getFunctionGroups);
    }


    /**
     * Delete all administrators for specified Service Agreement.
     *
     * @param serviceAgreement Service Agreement object with internal and external Ids specified.
     * @return Mono<Void>
     */
    public Mono<Void> deleteAdmins(ServiceAgreement serviceAgreement) {
        log.debug("Removing admins for Service Agreement {}", serviceAgreement.getName());
        return serviceAgreementServiceApi.getServiceAgreementAdmins(serviceAgreement.getInternalId())
            .flatMapMany(admins -> Flux.fromIterable(admins.getAdmins().stream().map(Admin::getUserId).toList()))
            // get External  ID for each admin.
            // We need to  get the user by using the internal id to facilitate the delete for issue #46
            .flatMap(userId -> usersApi.getUserById(userId, true)).map(GetUser::getExternalId)
            .collectList()
            .doOnNext(adminIds -> log.debug("Found  admins: {}", adminIds))
            .map(adminsExternalIds -> adminsExternalIds.stream()
                .map(adminId -> new ServiceAgreementAdmin()
                    .externalServiceAgreementId(serviceAgreement.getExternalId())
                    .externalUserId(adminId))
                .collect(Collectors.toList()))
            .flatMap(admins -> {
                if (CollectionUtils.isEmpty(admins)) {
                    return Mono.empty();
                } else {
                    return serviceAgreementIntegrationApi.batchUpdateServiceAgreementAdmins(
                            new ServiceAgreementAdminsBatchUpdateRequest()
                                .action(com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Action.REMOVE)
                                .users(admins))
                        .map(r -> batchResponseUtils.checkBatchResponseItem(r, "Delete Admin", r.getStatus().getValue(),
                            r.getResourceId(), r.getErrors()))
                        .collectList()
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("Failed to delete admin: {}", e.getResponseBodyAsString(), e);
                            return Mono.error(e);
                        }).then();
                }
            });
    }

    public Mono<Void> deleteAdmins(ServiceAgreementV2 serviceAgreement) {
        log.debug("Removing admins for Service Agreement {}", serviceAgreement.getName());
        return serviceAgreementServiceApi.getServiceAgreementAdmins(serviceAgreement.getInternalId())
            .flatMapMany(admins -> Flux.fromIterable(admins.getAdmins().stream().map(Admin::getUserId).toList()))
            // get External  ID for each admin.
            // We need to  get the user by using the internal id to facilitate the delete for issue #46
            .flatMap(userId -> usersApi.getUserById(userId, true)).map(GetUser::getExternalId)
            .collectList()
            .doOnNext(adminIds -> log.debug("Found  admins: {}", adminIds))
            .map(adminsExternalIds -> adminsExternalIds.stream()
                .map(adminId -> new ServiceAgreementAdmin()
                    .externalServiceAgreementId(serviceAgreement.getExternalId())
                    .externalUserId(adminId))
                .collect(Collectors.toList()))
            .flatMap(admins -> {
                if (CollectionUtils.isEmpty(admins)) {
                    return Mono.empty();
                } else {
                    return serviceAgreementIntegrationApi.batchUpdateServiceAgreementAdmins(
                            new ServiceAgreementAdminsBatchUpdateRequest()
                                .action(com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Action.REMOVE)
                                .users(admins))
                        .map(r -> batchResponseUtils.checkBatchResponseItem(r, "Delete Admin", r.getStatus().getValue(),
                            r.getResourceId(), r.getErrors()))
                        .collectList()
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("Failed to delete admin: {}", e.getResponseBodyAsString(), e);
                            return Mono.error(e);
                        }).then();
                }
            });
    }

    /**
     * Get internal identifiers of all arrangements mentioned in all Data Groups for specified Service Agreement.
     *
     * @param serviceAgreementInternalId internal identifier of Service Agreement.
     * @return flux of arrangements internal ids.
     */
    public Flux<String> getArrangementInternalIdsForServiceAgreement(String serviceAgreementInternalId) {
        return getExistingDataGroups(serviceAgreementInternalId, null)
            .mapNotNull(DataGroup::getItems)
            .flatMapIterable(Function.identity());
    }

    private List<BusinessFunctionGroup> getExistingBusinessGroups(
        List<BusinessFunctionGroup> businessFunctionGroups,
        List<FunctionGroupItem> functionGroups) {

        return businessFunctionGroups.stream()
            .filter(businessFunctionGroup -> filter(functionGroups, businessFunctionGroup))
            .map(businessFunctionGroup -> {
                enrich(functionGroups, businessFunctionGroup);
                return businessFunctionGroup;
            })
            .collect(Collectors.toList());
    }

    private void enrich(List<FunctionGroupItem> functionGroups,
        BusinessFunctionGroup businessFunctionGroup) {
        functionGroups.stream().filter(item -> productGroupAndDataGrouItemEquals(businessFunctionGroup, item))
            .findFirst()
            .ifPresent(item -> businessFunctionGroup.setId(item.getId()));
    }

    private boolean filter(List<FunctionGroupItem> functionGroups, BusinessFunctionGroup businessFunctionGroup) {
        return functionGroups.stream()
            .anyMatch(item -> productGroupAndDataGrouItemEquals(businessFunctionGroup, item));
    }

    private List<BusinessFunctionGroup> getNewBusinessGroups(List<BusinessFunctionGroup> businessFunctionGroups,
        List<FunctionGroupItem> functionGroups) {
        return businessFunctionGroups.stream()
            .filter(businessFunctionGroup -> isNewBusinessFunctionGroup(functionGroups, businessFunctionGroup))
            .collect(Collectors.toList());
    }

    private boolean isNewBusinessFunctionGroup(List<FunctionGroupItem> functionGroups,
        BusinessFunctionGroup businessFunctionGroup) {
        return functionGroups.stream()
            .noneMatch(item -> productGroupAndDataGrouItemEquals(businessFunctionGroup, item));
    }

    private Mono<BusinessFunctionGroup> createBusinessFunctionGroup(StreamTask streamTask,
        ServiceAgreement serviceAgreement, BusinessFunctionGroup businessFunctionGroup) {

        streamTask.info(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "create", serviceAgreement.getExternalId(), null,
            "Create new business function group: %s for service agreement: %s", businessFunctionGroup.getName(),
            serviceAgreement.getName());

        if (businessFunctionGroup.getDescription() == null) {
            businessFunctionGroup.setDescription(businessFunctionGroup.getName());
        }

        FunctionGroupCreateRequest functionGroupCreateRequest = new FunctionGroupCreateRequest();
        functionGroupCreateRequest.serviceAgreementId(serviceAgreement.getInternalId());
        functionGroupCreateRequest.setDescription(businessFunctionGroup.getDescription());
        functionGroupCreateRequest.setName(businessFunctionGroup.getName());
        functionGroupCreateRequest.setPermissions(getPermissions(businessFunctionGroup));
        functionGroupCreateRequest.setType(FunctionGroupCreateRequest.TypeEnum.CUSTOM);

        return functionGroupServiceApi.createFunctionGroup(functionGroupCreateRequest)
            .doOnError(WebClientResponseException.BadRequest.class, badRequest ->
                handleError(businessFunctionGroup, badRequest))
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(FUNCTION_GROUP, "ingest-function-groups", FAILED, streamTask.getName(), null,
                    badRequest, badRequest.getResponseBodyAsString(),
                    "Failed to setup Business Function Group: " + businessFunctionGroup.getName());
                return Mono.error(new StreamTaskException(streamTask, badRequest,
                    "Failed to setup Business Function Group: " + businessFunctionGroup.getName()
                        + " " + badRequest.getResponseBodyAsString()));
            })
            .doOnNext(idItem -> log.info("Created Business Function Group: {} with id: {}",
                businessFunctionGroup.getName(), idItem.getId()))
            .map(idItem -> {
                businessFunctionGroup.setId(idItem.getId());
                return businessFunctionGroup;
            });

    }

    private Mono<JobRole> createJobRole(StreamTask streamTask, ServiceAgreement serviceAgreement, JobRole jobRole) {

        streamTask.info(JOB_ROLE, SETUP_JOB_ROLE, "create", serviceAgreement.getExternalId(), null,
            "Create new job role: %s for service agreement: %s", jobRole.getName(), serviceAgreement.getName());

        if (jobRole.getDescription() == null) {
            jobRole.setDescription(jobRole.getName());
        }

        FunctionGroupCreateRequest presentationIngestFunctionGroup = accessGroupMapper.toPresentation(jobRole);
        presentationIngestFunctionGroup.setPermissions(accessGroupMapper.toPresentation(jobRole.getFunctionGroups()));
        presentationIngestFunctionGroup.serviceAgreementId(serviceAgreement.getInternalId());
        presentationIngestFunctionGroup.setMetadata(jobRole.getMetadata());

        //Logic to map the job roles types
        if (CollectionUtils.isEmpty(jobRole.getFunctionGroups())) {
            throw new IllegalArgumentException(
                String.format("Unexpected no function groups for job role: %s", jobRole.getName()));
        }
        final var jobRoleType = jobRole.getFunctionGroups().getFirst().getType();
        if (!ObjectUtils.isEmpty(jobRoleType)) {
            switch (jobRoleType) {
                case TEMPLATE:
                    presentationIngestFunctionGroup.setType(FunctionGroupCreateRequest.TypeEnum.REFERENCE);
                    break;
                case DEFAULT:
                    presentationIngestFunctionGroup.setType(FunctionGroupCreateRequest.TypeEnum.CUSTOM);
                    break;
                default:
                    throw new IllegalArgumentException(
                        String.format("Unexpected enum constant: %s for job role: %s", jobRoleType, jobRole.getName()));
            }
        } else {
            log.debug("No function group job role type is provided, creating a Local Job Role");
            presentationIngestFunctionGroup.setType(FunctionGroupCreateRequest.TypeEnum.CUSTOM);
        }

        return functionGroupServiceApi.createFunctionGroup(presentationIngestFunctionGroup)
            .doOnError(WebClientResponseException.BadRequest.class, badRequest ->
                handleError(jobRole, badRequest))
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(JOB_ROLE, "ingest-reference-job-role", FAILED, streamTask.getName(), null, badRequest,
                    badRequest.getResponseBodyAsString(),
                    "Failed to setup Job Role" + jobRole.getName());
                return Mono.error(new StreamTaskException(streamTask, badRequest,
                    "Failed to setup Job Role: " + jobRole.getName()
                        + " Response: " + badRequest.getResponseBodyAsString()));
            })
            .doOnNext(idItem -> log.info("Created Business Function Group: {} with id: {}",
                jobRole.getName(), idItem.getId()))
            .map(idItem -> {
                jobRole.setId(idItem.getId());
                return jobRole;
            });
    }

    private List<Permission> getPermissions(BusinessFunctionGroup businessFunctionGroup) {
        List<BusinessFunction> functions =
            Objects.requireNonNullElse(businessFunctionGroup.getFunctions(), Collections.emptyList());
        return functions.stream()
            .map(this::mapPresentationBusinessFunction)
            .collect(Collectors.toList());

    }

    private Permission mapPresentationBusinessFunction(BusinessFunction businessFunction) {
        Set<String> privileges = businessFunction.getPrivileges().stream()
            .map(Privilege::getPrivilege)
            .collect(Collectors.toSet());

        return new Permission().businessFunctionName(businessFunction.getName())
            .resourceName(businessFunction.getResourceName())
            .privileges(privileges);
    }

    Flux<AssignedPermission> getAssignedPermissions(ServiceAgreement serviceAgreement, User user, String
        resourceName, String functionName, String privilege) {
        return permissionCheckServiceApi.getDataItemPermissions(user.getInternalId(), serviceAgreement.getInternalId(),
                functionName, resourceName, privilege, null, null)
            .doOnError(WebClientResponseException.InternalServerError.class, this::handleError)
            .flatMapIterable(UserDataItemPermissionsList::getDataItems)
            .map(permissionsList -> mapAssignedPermission(resourceName, functionName, privilege,
                permissionsList.getDataItem().getId())
            );
    }

    private AssignedPermission mapAssignedPermission(
        String resourceName,
        String functionName,
        String privilege,
       String dataItemId) {
        AssignedPermission assignedPermission = new AssignedPermission();
        assignedPermission.setPermittedObjectInternalIds(List.of(dataItemId));
        assignedPermission.setFunctionName(functionName);
        assignedPermission.setResourceName(resourceName);
        assignedPermission.setPrivileges(new com.backbase.stream.legalentity.model.Privilege().privilege(privilege));
        return assignedPermission;
    }

    public Mono<List<JobRole>> setupJobRoleForSa(StreamTask streamTask, ServiceAgreement serviceAgreement,
        Stream<JobRole> jobRoleStream) {

        Mono<Map<String, FunctionGroupItem>> functionGroupsFromDb = getFunctionGroupsForServiceAgreement(
            serviceAgreement.getInternalId())
            .flatMapMany(Flux::fromIterable)
            .collectMap(FunctionGroupItem::getName, Function.identity());

        var concurrency = accessControlProperties.getConcurrency();

        return functionGroupsFromDb.flatMap(functionGroupsMap ->
            Flux.fromStream(jobRoleStream)
                .flatMap(jobRole -> {
                    FunctionGroupItem matchingGroup = functionGroupsMap.get(jobRole.getName());
                    if (matchingGroup == null) {
                        log.debug("Creating new Job Role: {}", jobRole.getName());
                        return createJobRole(streamTask, serviceAgreement, jobRole);
                    } else {
                        log.debug("Updating existing Job Role: {}", jobRole.getName());
                        return updateJobRole(streamTask, serviceAgreement, jobRole, matchingGroup);
                    }
                }, concurrency)
                .collectList()
        );
    }

    public Mono<JobRole> setupJobRole(StreamTask streamTask, ServiceAgreement masterServiceAgreement, JobRole jobRole) {
        streamTask.info(JOB_ROLE, SETUP_JOB_ROLE, "", masterServiceAgreement.getExternalId(),
            masterServiceAgreement.getInternalId(), "Setting up %s Job Role for Service Agreement: %s",
            jobRole.getName(), masterServiceAgreement.getName());
        log.info("Setup {} job role for Service Agreement: {}", jobRole.getName(),
            masterServiceAgreement.getExternalId());

        return getFunctionGroups(streamTask, masterServiceAgreement)
            .collectList()
            .flatMap(functionGroups -> {
                if (functionGroups.stream().noneMatch(fg -> fg.getName().equals(jobRole.getName()))) {
                    log.debug("New Job Role to create: {}", jobRole.getName());
                    return createJobRole(streamTask, masterServiceAgreement, jobRole);
                } else {
                    log.debug("Job Role Already exists: {}; Should be updated", jobRole.getName());
                    List<FunctionGroupItem> collect =
                        functionGroups.stream().filter(fg -> fg.getName().equals(jobRole.getName()))
                            .collect(Collectors.toList());
                    if (collect.isEmpty()) {
                        return Mono.just(jobRole);
                    }
                    if (collect.size() > 1) {
                        return Mono.error(new RuntimeException("More than one same job role"));
                    }
                    return updateJobRole(streamTask, masterServiceAgreement, jobRole, collect.get(0));
                }
            });
    }

    private Mono<? extends JobRole> updateJobRole(StreamTask streamTask, ServiceAgreement serviceAgreement,
        JobRole jobRole, FunctionGroupItem functionGroupItem) {

        log.info("Start Job Role updating: {}", functionGroupItem.getName());
        streamTask.info(JOB_ROLE, SETUP_JOB_ROLE, "update", serviceAgreement.getExternalId(), null,
            "Update job role: %s for service agreement: %s", jobRole.getName(), serviceAgreement.getName());

        if (jobRole.getDescription() == null) {
            jobRole.setDescription(jobRole.getName());
        }

        FunctionGroupBatchPutItem putRequestBody = new FunctionGroupBatchPutItem()
            .identifier(new FunctionGroupNameIdentifier().name(jobRole.getName())
                .serviceAgreementExternalId(serviceAgreement.getExternalId()))
            .newValues(new FunctionGroupUpdate()
                    .name(jobRole.getName())
                    .description(jobRole.getDescription())
//            .validFromDate(jobRole.getValidFromDate())
//            .validFromTime(jobRole.getValidFromTime())
//            .validUntilDate(jobRole.getValidUntilDate()) //todo VALID FROM/TO
//            .validUntilTime(jobRole.getValidUntilTime())
                    .permissions(accessGroupMapper.toUpdate(jobRole.getFunctionGroups()))
                    .metadata(jobRole.getMetadata())
            );

        var batchRequest = List.of(putRequestBody);

        if (isEmptyFunctionName(batchRequest)) {
            log.warn("Functions to Update do not contain functionName(it's required): {}", batchRequest);
            return Mono.just(jobRole);
        }
        log.debug("Function to Update: {}", Collections.singletonList(putRequestBody));

        return functionGroupIntegrationApi.batchUpdateFunctionGroups(batchRequest)
            .doOnError(WebClientResponseException.BadRequest.class, badRequest -> handleError(jobRole, badRequest))
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(JOB_ROLE, "ingest-reference-job-role", FAILED, streamTask.getName(), null, badRequest,
                    badRequest.getResponseBodyAsString(), "Failed to setup Job Role" + jobRole.getName());
                return Mono.error(new StreamTaskException(streamTask, badRequest,
                    "Failed to setup Job Role: " + jobRole.getName() + " Response: "
                        + badRequest.getResponseBodyAsString()));
            })
            .collectList()
            .flatMap(idItems -> {
                if (!CollectionUtils.isEmpty(idItems) && idItems.getFirst().getResourceId() != null) {
                    com.backbase.accesscontrol.functiongroup.api.integration.v1.model.BatchResponseItemExtended item = idItems.get(
                        0);
                    if (!item.getStatus().equals(StatusEnum.HTTP_STATUS_OK)) {
                        streamTask.error(JOB_ROLE, "ingest-reference-job-role", FAILED,
                            item.getExternalServiceAgreementId(), item.getResourceId(),
                            "Failed setting up Job Role: %s - status: %s, errors: %s",
                            jobRole.getName(), item.getStatus(), item.getErrors());
                        return Mono.error(
                            new StreamTaskException(streamTask, "Failed to setup Job Role: " + jobRole.getName()
                                + " - status: " + item.getStatus() + ", errors: " + item.getErrors()));
                    }
                    jobRole.setId(idItems.getFirst().getResourceId());
                }
                return Mono.just(jobRole);
            });
    }

    private Mono<List<com.backbase.accesscontrol.functiongroup.api.integration.v1.model.BatchResponseItemExtended>> updateBatchBusinessFunctionGroup(
        StreamTask streamTask,
        ServiceAgreement serviceAgreement, List<BusinessFunctionGroup> existingBusinessGroups) {

        log.info("Start Job Role updating: {}",
            existingBusinessGroups.stream().map(BusinessFunctionGroup::getName).collect(Collectors.toList()));

        streamTask.info(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "update", serviceAgreement.getExternalId(), null,
            "Update business function groups for service agreement: %s", serviceAgreement.getName());

        List<FunctionGroupBatchPutItem> presentationFunctionGroupPutRequestBody =
            existingBusinessGroups.stream()
                .filter(businessFunctionGroup -> !businessFunctionGroup.getType().equals(TypeEnum.TEMPLATE))
                .map(bfg -> new FunctionGroupBatchPutItem()
                    .identifier(new FunctionGroupNameIdentifier().name(bfg.getName())
                        .serviceAgreementExternalId(serviceAgreement.getExternalId()))
                    .newValues(new FunctionGroupUpdate()
                        .name(bfg.getName())
                        .description(Optional.ofNullable(bfg.getDescription()).orElse(bfg.getName()))
                        .permissions(accessGroupMapper.toUpdate(bfg))
                    ))
                .collect(Collectors.toList());

        if (isEmptyFunctionName(presentationFunctionGroupPutRequestBody)) {
            log.warn("Functions to Update doesn't contain functionName(it's required): {}",
                presentationFunctionGroupPutRequestBody);
            return Mono.just(Collections.emptyList());
        }
        log.debug("Functions to Update: {}", presentationFunctionGroupPutRequestBody);

        return functionGroupIntegrationApi.batchUpdateFunctionGroups(presentationFunctionGroupPutRequestBody)
            .doOnError(WebClientResponseException.BadRequest.class, this::handleError)
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask
                    .error(FUNCTION_GROUP, "update-function-groups", FAILED, streamTask.getName(), null, badRequest,
                        badRequest.getResponseBodyAsString(), "Failed to update Business Function Group");
                return Mono.error(new StreamTaskException(streamTask, badRequest,
                    "Failed to update Business Function Group: " + badRequest.getResponseBodyAsString()));
            })
            .collectList();
    }

    private boolean isEmptyFunctionName(List<FunctionGroupBatchPutItem> putRequestBodies) {
        return putRequestBodies.stream()
            .map(FunctionGroupBatchPutItem::getNewValues)
            .filter(Objects::nonNull)
            .map(FunctionGroupUpdate::getPermissions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission::getBusinessFunctionName)
            .anyMatch(Predicate.not(StringUtils::hasText));
    }

    /**
     * Retrieves the list of Service Agreements associated with a user by their internal ID.
     *
     * @param userInternalId the internal ID of the user
     * @return a Mono emitting a list of Service Agreements
     */
    public Mono<List<ServiceAgreement>> getUserContextsByUserId(String userInternalId) {
        log.debug("Getting Service Agreement for: {}", userInternalId);
        return fetchUserContexts(userInternalId)
            .flatMapIterable(
                com.backbase.accesscontrol.usercontext.api.service.v1.model.GetContexts::getContextServiceAgreements)
            .map(accessGroupMapper::toStream)
            .collectList()
            .doOnNext(serviceAgreements ->
                log.debug("[{}] Service Agreements found for user: {}", serviceAgreements.size(), userInternalId)
            )
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Failed to fetch service agreement by user internal id: {}", e.getResponseBodyAsString(), e);
                return Mono.error(e);
            });
    }

    private Flux<com.backbase.accesscontrol.usercontext.api.service.v1.model.GetContexts> fetchUserContexts(
        String userInternalId) {
        var currentPage = new AtomicInteger(0);
        var pageSize = accessControlProperties.getUserContextPageSize();
        return userContextApi.getUserContexts(userInternalId, null, currentPage.get(), pageSize)
            .expand(response -> {
                var totalPagesCalculated = (int) Math.ceil((double) response.getTotalCount() / pageSize);
                if (currentPage.incrementAndGet() >= totalPagesCalculated) {
                    return Mono.empty();
                }
                return userContextApi.getUserContexts(userInternalId, null, currentPage.get(), pageSize);
            });
    }

    @NotNull
    private Flux<FunctionGroupItem> getFunctionGroups(StreamTask streamTask, ServiceAgreement serviceAgreement) {
        return functionGroupServiceApi.getFunctionGroups(serviceAgreement.getInternalId())
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Failed to get Function Groups for Service Agreement: {} Response: {}",
                    serviceAgreement.getExternalId(), e.getResponseBodyAsString());
                streamTask.error(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "failed", serviceAgreement.getExternalId(),
                    serviceAgreement.getInternalId(),
                    "Failed to get function groups for Service Agreement: " + serviceAgreement.getInternalId());
                return Mono.error(new StreamTaskException(streamTask, e,
                    "Failed to get function groups for Service Agreement: " + serviceAgreement.getInternalId()));
            })
            .map(GetFunctionGroups::getFunctionGroups)
            .switchIfEmpty(Mono.just(Collections.emptyList()))
            .flatMapIterable(Function.identity());
    }

    private void handleError(WebClientResponseException badRequest) {
        log.warn("Error executing request: [{}] {}", badRequest.getRawStatusCode(),
            badRequest.getResponseBodyAsString());
    }

    private void handleError(BusinessFunctionGroup businessFunctionGroup, WebClientResponseException badRequest) {
        log.warn("Failed to create function group: {} Response: {}", businessFunctionGroup,
            badRequest.getResponseBodyAsString());
    }

    private void handleError(JobRole jobRole, WebClientResponseException badRequest) {
        log.warn("Failed to create job role: {} Response: {}", jobRole, badRequest.getResponseBodyAsString());
    }

    private record UserWithPermissions(User user, ServiceAgreement serviceAgreement,
                                       List<UserPermissionItem> permissionItems) {

    }
}
