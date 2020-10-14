package com.backbase.stream.service;

import com.backbase.dbs.accesscontrol.query.service.api.AccesscontrolApi;
import com.backbase.dbs.accesscontrol.query.service.model.DataGroupItem;
import com.backbase.dbs.accesscontrol.query.service.model.PersistenceApprovalPermissions;
import com.backbase.dbs.accesscontrol.query.service.model.SchemaFunctionGroupItem;
import com.backbase.dbs.accessgroup.presentation.service.api.AccessgroupsApi;
import com.backbase.dbs.accessgroup.presentation.service.model.ArrangementPrivilegesGetResponseBody;
import com.backbase.dbs.accessgroup.presentation.service.model.BatchResponseItemExtended;
import com.backbase.dbs.accessgroup.presentation.service.model.DataGroupItemSystemBase;
import com.backbase.dbs.accessgroup.presentation.service.model.IdItem;
import com.backbase.dbs.accessgroup.presentation.service.model.ListOfFunctionGroupsWithDataGroups;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationAction;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationApprovalStatus;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationAssignUserPermissions;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationDataGroupItemPutRequestBody;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationDataGroupUpdate;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationFunctionDataGroup;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationFunctionGroupDataGroup;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationGenericObjectId;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationIdentifier;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationIngestFunctionGroup;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationItemIdentifier;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationPermission;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationSearchDataGroupsRequest;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationServiceAgreementIdentifier;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationServiceAgreementUserPair;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationServiceAgreementUsersBatchUpdate;
import com.backbase.dbs.accessgroup.presentation.service.model.ServicesAgreementIngest;
import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.dbs.user.presentation.service.model.GetUserById;
import com.backbase.stream.legalentity.model.ApprovalStatus;
import com.backbase.stream.legalentity.model.AssignedPermission;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ReferenceJobRole;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.mapper.AccessGroupMapper;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.product.utils.BatchResponseUtils;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Access Group Service provide access to Access Control, Data Groups and Function Groups from a single service.
 */
@Slf4j
public class AccessGroupService {

    public static final String CREATE_ACCESS_GROUP = "create-access-group";
    public static final String ACCESS_GROUP = "access-group";
    public static final String REJECTED = "rejected";
    public static final String CREATED = "created";
    public static final String FUNCTION_GROUP = "function-group";
    public static final String REFERENCE_JOB_ROLE = "reference-job-role";
    public static final String SETUP_FUNCTION_GROUP = "setup-function-group";
    public static final String CREATE_FUNCTION_GROUP = "create-function-group";
    public static final String SETUP_REFERENCE_JOB_ROLE = "setup-reference-job-role";
    private static final String FAILED = "failed";

    public AccessGroupService(
        AccesscontrolApi accessControlApi,
        AccessgroupsApi accessGroupServiceApi,
        UsersApi usersApi) {
        this.accessControlApi = accessControlApi;
        this.accessGroupServiceApi = accessGroupServiceApi;
        this.usersApi = usersApi;
    }

    private final AccesscontrolApi accessControlApi;
    private final AccessgroupsApi accessGroupServiceApi;
    private final UsersApi usersApi;


    private final AccessGroupMapper accessGroupMapper = Mappers.getMapper(AccessGroupMapper.class);

    /**
     * Create Service Agreement.
     *
     * @param streamTask       The task invoking the call
     * @param serviceAgreement Service Agreement
     * @return Created Service Agreement
     */
    public Mono<ServiceAgreement> createServiceAgreement(StreamTask streamTask, ServiceAgreement serviceAgreement) {
        ServicesAgreementIngest servicesAgreementIngest = accessGroupMapper.toPresentation(serviceAgreement);
        return accessGroupServiceApi.postServiceAgreementIngest(servicesAgreementIngest)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                streamTask.error("service-agreement", "create", "failed", serviceAgreement.getExternalId(),
                    "", throwable, throwable.getResponseBodyAsString(), "Failed to create Service Agreement");
                return Mono.error(new StreamTaskException(streamTask, throwable, "Failed to create Service Agreement"));
            })
            .zipWith(Mono.just(serviceAgreement), storeIdInServiceAgreement());
    }

    private BiFunction<IdItem, ServiceAgreement, ServiceAgreement> storeIdInServiceAgreement() {
        return (idItem, serviceAgreement1) -> {
            serviceAgreement1.setInternalId(idItem.getId());
            log.info("Created Service Agreement: {} with id: {}", serviceAgreement1.getName(), idItem.getId());
            return serviceAgreement1;
        };
    }

    public Mono<LegalEntity> setAdministrators(LegalEntity legalEntity) {

        List<PresentationServiceAgreementUserPair> userPairs = legalEntity.getAdministrators().stream().map(user -> {
            PresentationServiceAgreementUserPair usersItem = new PresentationServiceAgreementUserPair();
            usersItem.setExternalServiceAgreementId(legalEntity.getMasterServiceAgreement().getExternalId());
            usersItem.setExternalUserId(user.getExternalId());
            return usersItem;
        }).collect(Collectors.toList());

        PresentationServiceAgreementUsersBatchUpdate presentationServiceAgreementUsersBatchUpdate = new PresentationServiceAgreementUsersBatchUpdate();
        presentationServiceAgreementUsersBatchUpdate.users(userPairs);
        presentationServiceAgreementUsersBatchUpdate.setAction(PresentationAction.ADD);

        return accessGroupServiceApi.putPresentationServiceAgreementAdminsBatchUpdate(presentationServiceAgreementUsersBatchUpdate)
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

        ListOfFunctionGroupsWithDataGroups functionGroups = functionGroupsWithDataGroup(jobProfileUser, productGroup);

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
                                                  JobProfileUser jobProfileUser, ListOfFunctionGroupsWithDataGroups functionGroups) {

        streamTask.info(ACCESS_GROUP, "assign-permissions", "",
            streamTask.getName(), null,
            "Assigning permissions for Service Agreement: %s", serviceAgreement.getExternalId());

        String serviceAgreementId = serviceAgreement.getInternalId();
        String userId = jobProfileUser.getUser().getInternalId();

        return accessGroupServiceApi.putAssignUsersPermissions(serviceAgreementId, userId, functionGroups)
            .onErrorResume(WebClientResponseException.class, e -> {
                streamTask.error(ACCESS_GROUP, "assign-permissions", "failed", serviceAgreement.getExternalId(),
                    serviceAgreementId, e, e.getResponseBodyAsString(), "Failed to to assign permissions");
                return Mono.error(new StreamTaskException(streamTask, e,
                    "Failed to Assign permissions: " + e.getResponseBodyAsString()));
            })
            .flatMap(this::processApprovalStatus)
            .map(jobProfileUser::approvalStatus);

    }

    public Mono<BatchProductGroupTask> assignPermissionsBatch(BatchProductGroupTask task, Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions) {

        log.info("\n****************** ASSIGNING PERMISSSION!! *********************\n: {}", usersPermissions);
        List<PresentationAssignUserPermissions> request = usersPermissions.keySet().stream()
            .map(user -> new PresentationAssignUserPermissions()
                .externalUserId(user.getExternalId())
                .externalServiceAgreementId(task.getData().getServiceAgreement().getExternalId())
                .functionGroupDataGroups(
                    usersPermissions.get(user).keySet().stream()
                        .map(bfg -> new PresentationFunctionGroupDataGroup()
                            .functionGroupIdentifier(mapId(bfg.getId()))
                            .dataGroupIdentifiers(
                                usersPermissions.get(user).get(bfg).stream()
                                    .map(pg -> mapId(pg.getInternalId()))
                                    .collect(Collectors.toList())))
                        .collect(Collectors.toList())))
            .collect(Collectors.toList());

        if (request.stream().anyMatch(usersPermission -> usersPermission.getFunctionGroupDataGroups().stream()
            .anyMatch(this::hasDataGroupIdentifiers))) {
            log.error("You are assigning permissions without data groups!!");
        }


        return Mono.just(request)
            .flatMap(userPermissionsRequest -> {
                if (task.getIngestionMode().equals(BatchProductGroupTask.IngestionMode.REPLACE)) {
                    task.info(ACCESS_GROUP, "assign-permissions", "", "", null, "Replacing assigned permissions for users: %s with: %s", prettyPrintExternalIds(userPermissionsRequest), prettyPrintDataGroups(userPermissionsRequest));
                    return Mono.just(userPermissionsRequest);
                }
                // request each user permissions and add those to the request.
                return Flux.fromIterable(usersPermissions.keySet())
                    .flatMap(user -> accessControlApi.getPersistenceApprovalPermissions(user.getInternalId(), task.getData().getServiceAgreement().getInternalId())
                        .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                        .map(PersistenceApprovalPermissions::getItems)
                        .map(existingUserPermissions -> {
                            log.info("Retrieved permissions for user with externalId {} : {}", user.getExternalId(), existingUserPermissions.stream().map(p -> p.getFunctionGroupId() + " : [" + p.getDataGroupIds() + "] ").collect(Collectors.toList()));

                            PresentationAssignUserPermissions requestUserPermissions = request.stream()
                                .filter(up -> up.getExternalUserId().equalsIgnoreCase(user.getExternalId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Permissions for user not present in request?"));

                            PresentationAssignUserPermissions mergedUserPermissions = new PresentationAssignUserPermissions();
                            mergedUserPermissions.setExternalServiceAgreementId(requestUserPermissions.getExternalServiceAgreementId());
                            mergedUserPermissions.setExternalUserId(requestUserPermissions.getExternalUserId());

                            if(existingUserPermissions.isEmpty()) {
                                mergedUserPermissions.setFunctionGroupDataGroups(requestUserPermissions.getFunctionGroupDataGroups());
                            } else {

                                // merge user permissions in  DBS with the ones already  in request.
                                existingUserPermissions.forEach(userPermission -> {

                                    String functionGroupId = userPermission.getFunctionGroupId();
                                    Set<String> dataGroupIds = new HashSet<>();
                                    if (userPermission.getDataGroupIds() != null) {
                                        dataGroupIds.addAll(userPermission.getDataGroupIds());
                                    }

                                    Optional<PresentationFunctionGroupDataGroup> requestedFunctionGroupOptional = requestUserPermissions.getFunctionGroupDataGroups().stream()
                                        .filter(requestFunctionGroup -> hasTheSameFunctionGroupId(functionGroupId, requestFunctionGroup))
                                        .findFirst();

                                    // If requested function group is already ingested, merge the request and existing function group
                                    if (requestedFunctionGroupOptional.isPresent()) {
                                        PresentationFunctionGroupDataGroup requestedFunctionGroup = requestedFunctionGroupOptional.get();

                                        if (requestedFunctionGroup.getDataGroupIdentifiers() != null) {
                                            dataGroupIds.addAll(requestedFunctionGroup.getDataGroupIdentifiers().stream().map(PresentationIdentifier::getIdIdentifier).collect(Collectors.toList()));
                                        }
                                    }

                                    // Transform existing permissions to PresentationFunctionGroupDataGroup
                                    PresentationFunctionGroupDataGroup functionGroup = new PresentationFunctionGroupDataGroup();
                                    functionGroup.setFunctionGroupIdentifier(mapId(functionGroupId));
                                    functionGroup.setDataGroupIdentifiers(dataGroupIds.stream().map(this::mapId).collect(Collectors.toList()));

                                    mergedUserPermissions.addFunctionGroupDataGroupsItem(functionGroup);
                                });
                            }
                            return mergedUserPermissions;
                        }))
                    .collectList()
                    .map(list -> {
                        log.info("Updated assigned permissions for users: {} with: {} ",
                            prettyPrintExternalIds(userPermissionsRequest),
                            prettyPrintDataGroups(userPermissionsRequest));
                        return list;
                    });
            })
            .flatMap(mergedRequest -> {
                task.info(ACCESS_GROUP, "assign-permissions", task.getName(), null, task.getId(), "Assigning permissions: %s", mergedRequest.stream().map(this::prettyPrintUserAssignedPermissions).collect(Collectors.joining(",")));
                return accessGroupServiceApi.putAssignUserPermissions(mergedRequest)
                    .map(r -> BatchResponseUtils.checkBatchResponseItem(r, "Permissions Update", r.getStatus().toString(), r.getResourceId(), r.getErrors()))
                    .doOnNext(r -> {
                        task.info(ACCESS_GROUP, "assign-permissions", r.getExternalServiceAgreementId(), null, "Assigned permissions for: %s and Service Agreement: %s", r.getResourceId(), r.getExternalServiceAgreementId());
                    })
                    .onErrorResume(WebClientResponseException.class, e -> {
                        task.error(ACCESS_GROUP, "assign-permissions", "failed", task.getData().getServiceAgreement().getExternalId(),
                            task.getData().getServiceAgreement().getInternalId(), e, e.getResponseBodyAsString(), "Failed to execute Batch Permissions assignment request.");
                        return Mono.error(new StreamTaskException(task, e, "Failed  to assign permissions: " + e.getResponseBodyAsString()));
                    })
                    .collectList();
            })
            .thenReturn(task);
    }

    private boolean hasTheSameFunctionGroupId(String functionGroupId, PresentationFunctionGroupDataGroup requestFunctionGroup) {
        return functionGroupId.equals(requestFunctionGroup.getFunctionGroupIdentifier().getIdIdentifier());
    }

    private String prettyPrint(PresentationFunctionGroupDataGroup functionGroup) {
        return " functionGroup: " + functionGroup.getFunctionGroupIdentifier().getIdIdentifier() +
            " dataGroupIds: " + prettyPrintPresentationDataGroups(functionGroup.getDataGroupIdentifiers());
    }

    private String prettyPrintUserAssignedPermissions(PresentationAssignUserPermissions presentationAssignUserPermissions) {
        return "User: " + presentationAssignUserPermissions.getExternalUserId() +
            " Service Agreement: " + presentationAssignUserPermissions.getExternalServiceAgreementId() +
            " Function & Data Groups: " + presentationAssignUserPermissions.getFunctionGroupDataGroups().stream().map(this::prettyPrint).collect(Collectors.joining(", "));

    }


    private String prettyPrintPresentationDataGroups(List<PresentationIdentifier> dataGroupIdentifiers) {
        if (dataGroupIdentifiers == null) {
            return "NO DATA GROUP IDS!";
        }
        return dataGroupIdentifiers.stream().map(PresentationIdentifier::getIdIdentifier).collect(Collectors.joining(","));
    }

    private boolean hasDataGroupIdentifiers(PresentationFunctionGroupDataGroup functionWithDataGroup) {
        return functionWithDataGroup.getDataGroupIdentifiers() != null
            && !functionWithDataGroup.getDataGroupIdentifiers().isEmpty();
    }

    private PresentationIdentifier mapId(String id) {
        return new PresentationIdentifier().idIdentifier(id);
    }

    private String prettyPrintDataGroups(List<PresentationAssignUserPermissions> r) {
        return r.stream().flatMap(presentationAssignUserPermissions ->
            presentationAssignUserPermissions.getFunctionGroupDataGroups().stream())
            .map(fdgd -> "functionGroupIdentifier: "
                + fdgd.getFunctionGroupIdentifier().getIdIdentifier()
                + " dataGroupItems: [" + fdgd.getDataGroupIdentifiers().stream()
                .map(PresentationIdentifier::getIdIdentifier)
                .collect(Collectors.joining(",")) + "]").collect(Collectors.joining(", "));
    }

    private String prettyPrintExternalIds(List<PresentationAssignUserPermissions> r) {
        return r.stream().map(PresentationAssignUserPermissions::getExternalUserId).collect(Collectors.joining(", "));
    }

    private ListOfFunctionGroupsWithDataGroups functionGroupsWithDataGroup(JobProfileUser jobProfileUser, ProductGroup productGroup) {
        /// Must also include existing function groupss!!

        ListOfFunctionGroupsWithDataGroups functionGroups = new ListOfFunctionGroupsWithDataGroups();
        List<PresentationFunctionDataGroup> collect = jobProfileUser.getBusinessFunctionGroups().stream()
            .map(businessFunctionGroup -> getPresentationFunctionDataGroup(productGroup, businessFunctionGroup))
            .collect(Collectors.toList());


        functionGroups.setItems(collect);
        return functionGroups;
    }

    private Mono<? extends ApprovalStatus> processApprovalStatus(
        PresentationApprovalStatus presentationApprovalStatus) {
        if (presentationApprovalStatus.getApprovalStatus() != null) {
            return Mono.just(accessGroupMapper.map(presentationApprovalStatus.getApprovalStatus()));
        } else {
            return Mono.empty();
        }
    }

    private PresentationFunctionDataGroup getPresentationFunctionDataGroup(ProductGroup productGroup,
                                                                           BusinessFunctionGroup businessFunctionGroup) {
        return new PresentationFunctionDataGroup()
            .functionGroupId(businessFunctionGroup.getId())
            .dataGroupIds(Collections.singletonList(new PresentationGenericObjectId().id(productGroup.getInternalId())));
    }

    /**
     * Link all products in Product Group with Service Agreement.
     *
     * @param streamTask Stream Task containing Product Group to link to Service Agreement
     * @return Product Group
     */
    public Mono<ProductGroupTask> setupProductGroups(ProductGroupTask streamTask) {
        streamTask.info(ACCESS_GROUP, "setup-product-group", "", streamTask.getProductGroup().getName(), null, "Setting up Data Group for: %s and Service Agreement: %s", streamTask.getData().getName(), streamTask.getData().getServiceAgreement().getExternalId());
        ProductGroup productGroup = streamTask.getData();
        ServiceAgreement serviceAgreement = productGroup.getServiceAgreement();

        return getExistingDataGroups(serviceAgreement.getInternalId(), productGroup.getProductGroupType().name())
            .doOnNext(dataGroupItem -> streamTask.info(ACCESS_GROUP, "setup-product-group", "exists", productGroup.getProductGroupType().name(), dataGroupItem.getId(), "Data Group already exists"))
            .onErrorResume(WebClientResponseException.BadRequest.class, e -> {
                streamTask.error(ACCESS_GROUP, "setup-product-group", "failed", serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), "Failed to setup Access Group groups for Service Agreement: " + serviceAgreement.getInternalId());
                return Mono.error(new StreamTaskException(streamTask, e, "Failed to setup Access Group for Service Agreement: " + serviceAgreement.getInternalId()));

            })
//            .filter(item -> isEquals(productGroup, item))
            .collectList()
            .doOnNext(dataGroupItem -> log.debug("Dataground found: {}", dataGroupItem))
            .flatMap(existingAccessGroups -> {
                Optional<DataGroupItem> first = existingAccessGroups.stream().filter(dataGroupItem -> dataGroupItem.getName().equals(productGroup.getName())).findFirst();
                return first.map(dataGroupItem -> updateAccessGroupWithArrangementIds(dataGroupItem, productGroup, streamTask))
                    .orElseGet(() -> createArrangementDataAccessGroup(serviceAgreement, streamTask.getProductGroup(), streamTask).cast(ProductGroup.class));
            }).map(streamTask::data);
    }

    public Mono<BatchProductGroupTask> updateExistingDataGroupsBatch(BatchProductGroupTask task, List<DataGroupItem> existingDataGroups, List<BaseProductGroup> productGroups) {
        List<PresentationDataGroupItemPutRequestBody> batchUpdateRequest = new ArrayList<>();
        final Set<String> affectedArrangements = productGroups.stream()
            .map(StreamUtils::getInternalProductIds)
            .flatMap(List::stream)
            .collect(Collectors.toSet());
        if (BatchProductGroupTask.IngestionMode.REPLACE.equals(task.getIngestionMode())) {
            // if REPLACE mode, existing products (not sent in the request) also need to be added to the set of affected arrangements.
            affectedArrangements.addAll(existingDataGroups.stream()
                .map(DataGroupItem::getItems)
                .flatMap(List::stream)
                .collect(Collectors.toSet())
            );
        }

        existingDataGroups.forEach(dbsDataGroup -> {
            // get group matching  DBS one.
            Optional<BaseProductGroup> pg = productGroups.stream()
                .filter(it -> isEquals(it, dbsDataGroup))
                .findFirst();
            List<String> arrangementsToAdd = new ArrayList<>();
            List<String> arrangementsToRemove = new ArrayList<>();
            affectedArrangements.forEach(arrangement -> {
                boolean shouldBeInGroup = pg.isPresent() && StreamUtils.getInternalProductIds(pg.get()).contains(arrangement);
                if (!dbsDataGroup.getItems().contains(arrangement) && shouldBeInGroup) {
                    // ADD.
                    log.debug("Arrangement item {} to be added to Data Group {}", arrangement, dbsDataGroup.getName());
                    arrangementsToAdd.add(arrangement);
                }
                if (dbsDataGroup.getItems().contains(arrangement) && !shouldBeInGroup) {
                    // remove.
                    log.debug("Arrangement item {} to be removed from Data Group {}", arrangement, dbsDataGroup.getName());
                    arrangementsToRemove.add(arrangement);
                }
            });
            if (!CollectionUtils.isEmpty(arrangementsToAdd)) {
                batchUpdateRequest.add(new PresentationDataGroupItemPutRequestBody()
                    .dataGroupIdentifier(mapId(dbsDataGroup.getId()))
                    .type(dbsDataGroup.getType())
                    .action(PresentationAction.ADD)
                    .dataItems(arrangementsToAdd.stream().map(id -> new PresentationItemIdentifier().internalIdIdentifier(id)).collect(Collectors.toList()))
                );
            }
            if (!CollectionUtils.isEmpty(arrangementsToRemove)) {
                batchUpdateRequest.add(new PresentationDataGroupItemPutRequestBody()
                    .dataGroupIdentifier(mapId(dbsDataGroup.getId()))
                    .type(dbsDataGroup.getType())
                    .action(PresentationAction.REMOVE)
                    .dataItems(arrangementsToRemove.stream().map(id -> new PresentationItemIdentifier().internalIdIdentifier(id)).collect(Collectors.toList()))
                );
            }
        });
        if (!CollectionUtils.isEmpty(batchUpdateRequest)) {
            return updateDataGroupItems(batchUpdateRequest)
                .doOnNext(response ->
                    task.info(ACCESS_GROUP, "update", response.getStatus().toString(), response.getResourceId(), null, "Product group updated.")
                )
                .onErrorResume(WebClientResponseException.class, e -> {
                    task.error(ACCESS_GROUP, "product-group", "failed", task.getData().getServiceAgreement().getExternalId(),
                        task.getData().getServiceAgreement().getInternalId(), e, e.getResponseBodyAsString(), "Failed to update product groups");
                    return Mono.error(new StreamTaskException(task, e, "Failed Update Product groups: " + e.getResponseBodyAsString()));
                })
                .collectList()
                .thenReturn(task);
        } else {
            log.debug("All Product Groups are up to date.");
            task.info(ACCESS_GROUP, "update", "SUCCESS", prettyPrintProductGroupNames(task), null, "All Product Groups are up to date.");
            return Mono.just(task);
        }
    }

    @NotNull
    private String prettyPrintProductGroupNames(BatchProductGroupTask task) {
        return task.getBatchProductGroup().getProductGroups().stream().map(BaseProductGroup::getName).collect(Collectors.joining(","));
    }

    public Flux<BatchResponseItemExtended> updateDataGroupItems(List<PresentationDataGroupItemPutRequestBody> request) {
        return accessGroupServiceApi.putDataGroupItemsUpdate(request)
            .map(r -> BatchResponseUtils.checkBatchResponseItem(r, "Product Groups Update", r.getStatus().toString(), r.getResourceId(), r.getErrors()));
    }

    public Flux<DataGroupItem> getExistingDataGroups(String serviceAgreementInternalId, String type) {
        return accessControlApi.getDataGroups(serviceAgreementInternalId, type, true);
    }

    /**
     * Return Data Group Items linked to a Service Agreement.
     *
     * @param serviceAgreementId Service Agreement Internal ID
     * @return List of IDs
     */
    @SuppressWarnings("WeakerAccess")
    public Flux<String> getDataGroupItemIdsByServiceAgreementId(String serviceAgreementId) {
        PresentationServiceAgreementIdentifier serviceAgreementIdentifier =
            new PresentationServiceAgreementIdentifier().idIdentifier(serviceAgreementId);
        return getDataGroupItemIds(null, serviceAgreementIdentifier);
    }

    /**
     * Setup Business Function Groups and link them to the Service Agreement.
     *
     * @param streamTask
     * @param serviceAgreement       Service Agreement
     * @param businessFunctionGroups Business function groups to connect to service agreement.
     * @return Job Profile User with updated Function Groups
     */
    public Mono<List<BusinessFunctionGroup>> setupFunctionGroups(StreamTask streamTask, ServiceAgreement serviceAgreement, List<BusinessFunctionGroup> businessFunctionGroups) {
        streamTask.info(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "", serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), "Setting up %s Business Functions for Service Agreement: %s", businessFunctionGroups.size(), serviceAgreement.getName());
        log.info("Setup {} Business Function for Service Agreement: {}", businessFunctionGroups.size(), serviceAgreement.getExternalId());
        return accessControlApi.getFunctionGroups(serviceAgreement.getInternalId())
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Failed to get Function Groups for Service Agreement: {} Response: {}", serviceAgreement.getExternalId(), e.getResponseBodyAsString());
                streamTask.error(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "failed", serviceAgreement.getExternalId(), serviceAgreement.getInternalId(), "Failed to get function groups for Service Agreement: " + serviceAgreement.getInternalId());
                return Mono.error(new StreamTaskException(streamTask, e, "Failed to get function groups for Service Agreement: " + serviceAgreement.getInternalId()));
            })
            .collectList()
            .flatMap(functionGroups -> {
                    List<BusinessFunctionGroup> newBusinessGroups = getNewBusinessGroups(businessFunctionGroups, functionGroups);
                    return Flux.fromIterable(newBusinessGroups)
                        .flatMap(bfg -> createBusinessFunctionGroup(streamTask, serviceAgreement, bfg))
                        .collectList()
                        .map(bfg -> {
                            bfg.addAll(getExistingBusinessGroups(businessFunctionGroups, functionGroups));
                            return bfg;
                        });
                }
            );

    }

    /**
     * Retrieve list of IDs from external Service Agreement ID.
     *
     * @param externalServiceAgreementId External Service Agreement ID
     * @param type                       Type of objects to retrieve
     * @return list of ids
     */

    public Flux<String> getDataGroupItemIdsByExternalServiceAgreementId(String externalServiceAgreementId,
                                                                        String type) {
        PresentationServiceAgreementIdentifier serviceAgreementIdentifier =
            new PresentationServiceAgreementIdentifier().externalIdIdentifier(externalServiceAgreementId);
        return getDataGroupItemIds(type, serviceAgreementIdentifier);
    }

    public boolean isEquals(BaseProductGroup productGroup, DataGroupItem item) {
        return item.getName().equals(productGroup.getName());
    }

    private boolean productGroupAndDataGrouItemEquals(BusinessFunctionGroup businessFunctionGroup,
                                                      SchemaFunctionGroupItem getFunctionGroupsFunctionGroupItem) {
        return getFunctionGroupsFunctionGroupItem.getName().equals(businessFunctionGroup.getName());
    }


    private Mono<ProductGroup> updateAccessGroupWithArrangementIds(DataGroupItem dataGroupsDataGroupItem,
                                                                   ProductGroup productGroup, StreamTask streamTask) {

        streamTask.info(ACCESS_GROUP, "update-access-group", "", dataGroupsDataGroupItem.getName(), dataGroupsDataGroupItem.getId(), "Updating Data Group");

        log.info("Updating Data Access Group: {}", dataGroupsDataGroupItem.getId());

        List<PresentationItemIdentifier> dataItems = StreamUtils.getInternalProductIds(productGroup)
            .stream().map(id -> new PresentationItemIdentifier().internalIdIdentifier(id)).collect(Collectors.toList());

        PresentationDataGroupUpdate presentationDataGroupUpdate = new PresentationDataGroupUpdate();
        presentationDataGroupUpdate.setDataGroupIdentifier(mapId(dataGroupsDataGroupItem.getId()));
        presentationDataGroupUpdate.setDataItems(dataItems);
        presentationDataGroupUpdate.setDescription(dataGroupsDataGroupItem.getDescription());
        presentationDataGroupUpdate.setName(dataGroupsDataGroupItem.getName());
        presentationDataGroupUpdate.setType(dataGroupsDataGroupItem.getType());

        return accessGroupServiceApi.putDataGroups(presentationDataGroupUpdate)
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(ACCESS_GROUP, "update-access-group", "failed", productGroup.getName(), dataGroupsDataGroupItem.getId(), badRequest, badRequest.getResponseBodyAsString(), "Failed to update access group");
                log.error("Error Updating data access group: {}", badRequest.getResponseBodyAsString());
                return Mono.error(new StreamTaskException(streamTask, badRequest, "Failed to update Data Access Group"));
            })
            .thenReturn(productGroup)
            .map(pg -> {
                streamTask.info(ACCESS_GROUP, "update-access-group", "updated", pg.getName(), dataGroupsDataGroupItem.getId(),
                    "Updated Data Access Group: %s", pg.getName());
                log.info("Updated Data Access Group: {}", pg.getName());
                pg.setInternalId(dataGroupsDataGroupItem.getId());
                return pg;
            });
    }


    public Mono<BaseProductGroup> createArrangementDataAccessGroup(ServiceAgreement serviceAgreement, BaseProductGroup productGroup, StreamTask streamTask) {
        streamTask.info(ACCESS_GROUP, CREATE_ACCESS_GROUP, "", productGroup.getName(), null, "Create new Data Group");

        log.info("Creating Data Access Group: {}", productGroup.getName());

        List<String> productIds = StreamUtils.getInternalProductIds(productGroup);
        DataGroupItemSystemBase dataGroupItemSystemBase = new DataGroupItemSystemBase();
        dataGroupItemSystemBase.setName(productGroup.getName());
        dataGroupItemSystemBase.setDescription(productGroup.getDescription());
        dataGroupItemSystemBase.setServiceAgreementId(serviceAgreement.getInternalId());
        dataGroupItemSystemBase.setAreItemsInternalIds(true);
        dataGroupItemSystemBase.setItems(productIds);
        dataGroupItemSystemBase.setType(productGroup.getProductGroupType().name());
        if (dataGroupItemSystemBase.getItems().stream().anyMatch(Objects::isNull)) {
            streamTask.error(ACCESS_GROUP, CREATE_ACCESS_GROUP, REJECTED, productGroup.getName(), null, "Data group items cannot have null items");
            throw new StreamTaskException(streamTask, "Data Group Items cannot have null items");
        }

        return accessGroupServiceApi.postDataGroups(dataGroupItemSystemBase)
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(ACCESS_GROUP, CREATE_ACCESS_GROUP, REJECTED, productGroup.getName(), null, "Data group items cannot have null items");
                return Mono.error(new StreamTaskException(streamTask, badRequest, "Data Group Items cannot have null items"));
            })
            .map(idItem -> {
                    streamTask.info(ACCESS_GROUP, CREATE_ACCESS_GROUP, CREATED, productGroup.getName(), idItem.getId(), "Create new Data Group");
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
        log.debug("Removing permissions from all Data Groups for user: {} in service agreement {}", userInternalId, serviceAgreementInternalId);
        return accessGroupServiceApi.putAssignUsersPermissions(
            serviceAgreementInternalId,
            userInternalId,
            new ListOfFunctionGroupsWithDataGroups())
            .then();
    }

    /**
     * Delete all Function Groups  defined in  service agreement.
     *
     * @param serviceAgreementInternalId Sevice Agreement internal idenetifier.
     * @return Mono<Void>
     */
    public Mono<Void> deleteFunctionGroupsForServiceAgreement(String serviceAgreementInternalId) {
        log.debug("Retrieving Function Groups for Service Agreement {}", serviceAgreementInternalId);
        return accessControlApi.getFunctionGroups(serviceAgreementInternalId)
            .collectList()
            .flatMap(functionGroups ->
                accessGroupServiceApi.postFunctionGroupsDelete(
                    functionGroups.stream().map(fg -> mapId(fg.getId())).collect(Collectors.toList()))
                    .map(r -> BatchResponseUtils.checkBatchResponseItem(r, "Function  Group Removal", r.getStatus().getValue(), r.getResourceId(), r.getErrors()))
                    .collectList())
            .then();
    }

    /**
     * Retrieve all Reference Job Roles defined in  service agreement.
     *
     * @param serviceAgreementInternalId Sevice Agreement internal idenetifier.
     * @return Mono<List < GetFunctionGroupsFunctionGroupItem>>
     */
    public Mono<List<SchemaFunctionGroupItem>> getFunctionGroupsForServiceAgreement(String serviceAgreementInternalId) {
        log.debug("Retrieving Function Groups for Service Agreement {}", serviceAgreementInternalId);
        return accessControlApi.getFunctionGroups(serviceAgreementInternalId)
            .collectList();
    }


    /**
     * Delete all administrators for specified Service Agreement.
     *
     * @param serviceAgreement Service Agreement object with internal and external Ids specified.
     * @return Mono<Void>
     */
    public Mono<Void> deleteAdmins(ServiceAgreement serviceAgreement) {
        log.debug("Removing admins for Service Agreement {}", serviceAgreement.getName());
        return accessControlApi.getServiceAgreementAdmins(serviceAgreement.getInternalId())
            .flatMapMany(admins -> Flux.fromIterable(admins.getAdmins()))
            // get External  ID for each admin.
            .flatMap(usersApi::getUserByExternalIdgetUserByExternalId)
            .map(GetUserById::getExternalId)
            .collectList()
            .doOnNext(adminIds -> log.debug("Found  admins: {}", adminIds))
            .map(adminsExternalIds -> adminsExternalIds.stream()
                .map(adminId -> new PresentationServiceAgreementUserPair()
                    .externalServiceAgreementId(serviceAgreement.getExternalId())
                    .externalUserId(adminId))
                .collect(Collectors.toList()))
            .flatMap(admins -> {
                if (CollectionUtils.isEmpty(admins)) {
                    return Mono.empty();
                } else {
                    return accessGroupServiceApi.putPresentationServiceAgreementAdminsBatchUpdate(
                        new PresentationServiceAgreementUsersBatchUpdate()
                            .action(PresentationAction.REMOVE)
                            .users(admins))
                        .map(r -> BatchResponseUtils.checkBatchResponseItem(r, "Delete Admin", r.getStatus().getValue(), r.getResourceId(), r.getErrors()))
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
        return accessControlApi.getDataGroups(serviceAgreementInternalId, null, true)
            .collectList()
            .map(dataGroupItems -> {
                // get all internal arrangement IDs present in data groups.
                Set<String> internalIds = new HashSet<>();
                dataGroupItems.forEach(dataGroupItem -> internalIds.addAll(dataGroupItem.getItems()));
                return new ArrayList<>(internalIds);
            })
            .flatMapMany(Flux::fromIterable);
    }

    private List<BusinessFunctionGroup> getExistingBusinessGroups(
        List<BusinessFunctionGroup> businessFunctionGroups,
        List<SchemaFunctionGroupItem> functionGroups) {

        return businessFunctionGroups.stream()
            .filter(businessFunctionGroup -> filter(functionGroups, businessFunctionGroup))
            .peek(businessFunctionGroup -> enrich(functionGroups, businessFunctionGroup))
            .collect(Collectors.toList());
    }

    private void enrich(List<SchemaFunctionGroupItem> functionGroups,
                        BusinessFunctionGroup businessFunctionGroup) {
        functionGroups.stream().filter(item -> productGroupAndDataGrouItemEquals(businessFunctionGroup, item)).findFirst()
            .ifPresent(item -> businessFunctionGroup.setId(item.getId()));
    }

    private boolean filter(List<SchemaFunctionGroupItem> functionGroups, BusinessFunctionGroup businessFunctionGroup) {
        return functionGroups.stream()
            .anyMatch(item -> productGroupAndDataGrouItemEquals(businessFunctionGroup, item));
    }

    private List<BusinessFunctionGroup> getNewBusinessGroups(List<BusinessFunctionGroup> businessFunctionGroups,
                                                             List<SchemaFunctionGroupItem> functionGroups) {
        return businessFunctionGroups.stream()
            .filter(businessFunctionGroup -> isNewBusinessFunctionGroup(functionGroups, businessFunctionGroup))
            .collect(Collectors.toList());
    }

    private boolean isNewBusinessFunctionGroup(List<SchemaFunctionGroupItem> functionGroups,
                                               BusinessFunctionGroup businessFunctionGroup) {
        return functionGroups.stream().noneMatch(item -> productGroupAndDataGrouItemEquals(businessFunctionGroup, item));
    }

    private Mono<BusinessFunctionGroup> createBusinessFunctionGroup(StreamTask streamTask, ServiceAgreement serviceAgreement,
                                                                    BusinessFunctionGroup businessFunctionGroup) {

        streamTask.info(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "create", serviceAgreement.getExternalId(), null, "Create new business function group: %s for service agreement: %s", businessFunctionGroup.getName(), serviceAgreement.getName());

        if (businessFunctionGroup.getDescription() == null) {
            businessFunctionGroup.setDescription(businessFunctionGroup.getName());
        }

        PresentationIngestFunctionGroup presentationIngestFunctionGroup = new PresentationIngestFunctionGroup();
        presentationIngestFunctionGroup.setExternalServiceAgreementId(serviceAgreement.getExternalId());
        presentationIngestFunctionGroup.setDescription(businessFunctionGroup.getDescription());
        presentationIngestFunctionGroup.setName(businessFunctionGroup.getName());
        presentationIngestFunctionGroup.setPermissions(getPresentationPermissions(businessFunctionGroup));
        presentationIngestFunctionGroup.setType(PresentationIngestFunctionGroup.TypeEnum.REGULAR);

        return accessGroupServiceApi.postPresentationIngestFunctionGroup(presentationIngestFunctionGroup)
            .doOnError(WebClientResponseException.BadRequest.class, badRequest ->
                handleError(businessFunctionGroup, badRequest))
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(FUNCTION_GROUP, "ingest-function-groups", FAILED, streamTask.getName(), null, badRequest, badRequest.getResponseBodyAsString(), "Failed to setup Business Function Group");
                return Mono.error(new StreamTaskException(streamTask, badRequest, "Failed to setup Business Function Group: " + badRequest.getResponseBodyAsString()));
            })
            .doOnNext(idItem -> log.info("Created Business Function Group: {} with id: {}",
                businessFunctionGroup.getName(), idItem.getId()))
            .map(idItem -> {
                businessFunctionGroup.setId(idItem.getId());
                return businessFunctionGroup;
            });

    }

    private Mono<ReferenceJobRole> createReferenceJobRole(StreamTask streamTask, ServiceAgreement serviceAgreement,
                                                          ReferenceJobRole referenceJobRole) {

        streamTask.info(REFERENCE_JOB_ROLE, SETUP_REFERENCE_JOB_ROLE, "create", serviceAgreement.getExternalId(), null, "Create new Reference job role: %s for service agreement: %s", referenceJobRole.getName(), serviceAgreement.getName());

        if (referenceJobRole.getDescription() == null) {
            referenceJobRole.setDescription(referenceJobRole.getName());
        }

        PresentationIngestFunctionGroup presentationIngestFunctionGroup =
            accessGroupMapper.toPresentation(referenceJobRole);
        presentationIngestFunctionGroup
            .setPermissions(accessGroupMapper.toPresentation(referenceJobRole.getFunctionGroups()));

        presentationIngestFunctionGroup.setExternalServiceAgreementId(serviceAgreement.getExternalId());

        return accessGroupServiceApi.postPresentationIngestFunctionGroup(presentationIngestFunctionGroup)
            .doOnError(WebClientResponseException.BadRequest.class, badRequest ->
                handleError(referenceJobRole, badRequest))
            .onErrorResume(WebClientResponseException.class, badRequest -> {
                streamTask.error(REFERENCE_JOB_ROLE, "ingest-reference-job-role", FAILED, streamTask.getName(), null, badRequest, badRequest.getResponseBodyAsString(), "Failed to setup Reference Job Role");
                return Mono.error(new StreamTaskException(streamTask, badRequest, "Failed to setup Reference Job Role: " + badRequest.getResponseBodyAsString()));
            })
            .doOnNext(idItem -> log.info("Created Business Function Group: {} with id: {}",
                referenceJobRole.getName(), idItem.getId()))
            .map(idItem -> {
                referenceJobRole.setId(idItem.getId());
                return referenceJobRole;
            });
    }

    private void handleError(WebClientResponseException badRequest) {
        log.warn("Error executing request: [{}] {}", badRequest.getRawStatusCode(), badRequest.getResponseBodyAsString());
    }

    private void handleError(BusinessFunctionGroup businessFunctionGroup, WebClientResponseException badRequest) {
        log.warn("Failed to create function group: {} Response: {}", businessFunctionGroup, badRequest.getResponseBodyAsString());
    }

    private void handleError(ReferenceJobRole referenceJobRole, WebClientResponseException badRequest) {
        log.warn("Failed to create reference job role: {} Response: {}", referenceJobRole, badRequest.getResponseBodyAsString());
    }

    private List<PresentationPermission> getPresentationPermissions(BusinessFunctionGroup businessFunctionGroup) {
        return businessFunctionGroup.getFunctions().stream()
            .map(this::mapPresentationBusinessFunction)
            .collect(Collectors.toList());

    }

    private PresentationPermission mapPresentationBusinessFunction(BusinessFunction businessFunction) {

        List<String> privileges;
        privileges = businessFunction.getPrivileges().stream()
            .map(Privilege::getPrivilege)
            .collect(Collectors.toList());

        return new PresentationPermission()
            .functionId(businessFunction.getFunctionId())
            .privileges(privileges);
    }


    Flux<AssignedPermission> getAssignedPermissions(
        ServiceAgreement serviceAgreement, User user, String
        resourceName, String functionName, String privilege) {


        return accessGroupServiceApi.getArrangementPrivileges(user.getInternalId(), functionName, resourceName, serviceAgreement.getInternalId(), privilege)
            .doOnError(WebClientResponseException.InternalServerError.class, this::handleError)
            .map(arrangementPrivilege -> mapAssignedPermission(
                resourceName,
                functionName,
                privilege,
                arrangementPrivilege)
            );
    }

    private AssignedPermission mapAssignedPermission(
        String resourceName,
        String functionName,
        String privilege,
        ArrangementPrivilegesGetResponseBody arrangementPrivilege) {
        AssignedPermission assignedPermission = new AssignedPermission();
        List<String> permittedObjectInternalIds = Collections.singletonList(arrangementPrivilege.getArrangementId());
        assignedPermission.setPermittedObjectInternalIds(permittedObjectInternalIds);
        assignedPermission.setFunctionName(functionName);
        assignedPermission.setResourceName(resourceName);
        assignedPermission.setPrivileges(new com.backbase.stream.legalentity.model.Privilege().privilege(privilege));
        return assignedPermission;
    }


    @SuppressWarnings("ConstantConditions")
    private Flux<String> getDataGroupItemIds(String type,
                                             PresentationServiceAgreementIdentifier serviceAgreementIdentifier) {
        return accessGroupServiceApi.postSearch(type,
            new PresentationSearchDataGroupsRequest().serviceAgreementIdentifier(serviceAgreementIdentifier))
            .flatMap(item -> {
                if (item.getDataGroups() != null) {
                    return Flux.fromIterable(item.getDataGroups());
                } else {
                    return Flux.empty();
                }
            })
            .flatMap(details -> getExistingDataGroups(details.getId(), null))
            .flatMap(dataGroupItem -> Flux.fromIterable(dataGroupItem.getItems()));
    }

    public Mono<ReferenceJobRole> setupReferenceJobRole(StreamTask streamTask,
                                                        ServiceAgreement masterServiceAgreement, ReferenceJobRole referenceJobRole) {
        streamTask.info(REFERENCE_JOB_ROLE, SETUP_REFERENCE_JOB_ROLE, "", masterServiceAgreement.getExternalId(),
            masterServiceAgreement.getInternalId(), "Setting up %s Reference Job Role for Service Agreement: %s",
            referenceJobRole.getName(), masterServiceAgreement.getName());
        log.info("Setup {} Reference job role for Service Agreement: {}", referenceJobRole.getName(),
            masterServiceAgreement.getExternalId());

        return accessControlApi.getFunctionGroups(masterServiceAgreement.getInternalId())
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Failed to get Function Groups for Service Agreement: {} Response: {}",
                    masterServiceAgreement.getExternalId(), e.getResponseBodyAsString());
                streamTask.error(FUNCTION_GROUP, SETUP_FUNCTION_GROUP, "failed", masterServiceAgreement.getExternalId(),
                    masterServiceAgreement.getInternalId(),
                    "Failed to get function groups for Service Agreement: " + masterServiceAgreement.getInternalId());
                return Mono.error(new StreamTaskException(streamTask, e,
                    "Failed to get function groups for Service Agreement: " + masterServiceAgreement.getInternalId()));
            })
            .collectList()
            .flatMap(functionGroups -> {
                if (functionGroups.stream().noneMatch(fg -> fg.getName().equals(referenceJobRole.getName()))) {
                    log.debug("Reference Job Role Already exist: {}", referenceJobRole.getName());
                    return createReferenceJobRole(streamTask, masterServiceAgreement, referenceJobRole);
                }
                return Mono.just(referenceJobRole);
            });
    }

}
