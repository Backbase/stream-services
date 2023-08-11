package com.backbase.stream.service;

import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.*;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.identity.integration.api.service.v1.model.EnhancedUserRepresentation;
import com.backbase.identity.integration.api.service.v1.model.UserRequestBody;
import com.backbase.stream.exceptions.UserUpsertException;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.*;
import com.backbase.stream.mapper.RealmMapper;
import com.backbase.stream.mapper.UserMapper;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;

/**
 * Stream User Management. Still needs to be adapted to use Identity correctly
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@AllArgsConstructor
public class UserService {

    public static final String REMOVED_PREFIX = "REMOVED_";
    public static final String ARCHIVED_PREFIX = "archived_";

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);
    private final RealmMapper realmMapper = Mappers.getMapper(RealmMapper.class);

    private final UserManagementApi usersApi;
    private final IdentityManagementApi identityManagementApi;
    private final Optional<IdentityIntegrationServiceApi> identityIntegrationApi;

    /**
     * Get User by external ID.
     *
     * @param externalId External ID
     * @return User if exists. Empty if not.
     */
    public Mono<User> getUserByExternalId(String externalId) {
        return usersApi.getUserByExternalId(externalId, true)
            .doOnNext(userItem -> log.info("Found user: {} for externalId: {}", userItem.getFullName(), userItem.getExternalId()))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                handleUserNotFound(externalId, notFound.getResponseBodyAsString()))
            .map(mapper::toStream);
    }

    private Mono<? extends GetUser> handleUserNotFound(String externalId, String responseBodyAsString) {
        log.info("User with externalId: {} does not exist: {}", externalId, responseBodyAsString);
        return Mono.empty();
    }

    public Mono<User> createUser(User user, String legalEntityExternalId, StreamTask streamTask) {
        UserExternal createUser = mapper.toPresentation(user);
        createUser.setLegalEntityExternalId(legalEntityExternalId);

        return usersApi.createUser(createUser)
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Error creating user: {} Response: {}", user.getExternalId(), e.getResponseBodyAsString());
                String message = "Failed to create user: " + user.getExternalId();
                streamTask.error("user", "create-user", "failed",
                    legalEntityExternalId, user.getExternalId(), e, e.getMessage(), message);

                return Mono.error(new StreamTaskException(streamTask, message));
            })
            .map(userCreated -> handleCreateUserResult(user, userCreated));
    }

    /**
     * Get users for specified legal entity.
     *
     * @param legalEntityInternalId legal  entity internal id.
     * @return flux of user  items.
     */
    public Mono<GetUsersList> getUsersByLegalEntity(String legalEntityInternalId, int size, int from) {
        log.debug("Retrieving users for Legal Entity '{}'", legalEntityInternalId);

        GetUsersByLegalEntityIdsRequest request = new GetUsersByLegalEntityIdsRequest();
        request.addLegalEntityIdsItem(legalEntityInternalId);
        request.size(size);
        request.from(from);
        return usersApi.getUsersByLegalEntityIds(request, true);
    }

    /**
     * Archive users. As it is not possible to remove users from DBS, to be able to remove Legal Entity
     * user external ID is being updated to random value. (REMOVED_<external_id>_UUID)
     * This step is required to be able re-ingest user with same external id again.
     *
     * @param legalEntityInternalId
     * @param userExternalIds
     * @return Mono<Void>
     */
    public Mono<Void> archiveUsers(String legalEntityInternalId, List<String> userExternalIds) {
        //  There is no way to remove user from DBS, so to bypass this we just archive DBS user representing member.
        return usersApi.updateUserInBatch(
                userExternalIds.stream()
                    .map(userExternalId -> {
                        return new BatchUser()
                            .externalId(userExternalId)
                            .userUpdate(new com.backbase.dbs.user.api.service.v2.model.User()
                                .externalId(REMOVED_PREFIX + userExternalId + "_" + UUID.randomUUID())
                                .legalEntityId(legalEntityInternalId)
                                .fullName(ARCHIVED_PREFIX + userExternalId));
                    })
                    .collect(Collectors.toList()))
            .map(r -> {
                log.debug("Batch Archive User response: status {} for resource {}, errors: {}", r.getStatus(), r.getResourceId(), r.getErrors());
                if (r.getStatus().getValue() != null && !HttpStatus.valueOf(Integer.parseInt(r.getStatus().getValue())).is2xxSuccessful()) {
                    throw new RuntimeException(
                        MessageFormat.format("Failed item in the batch for User Update: status {0} for resource {1}, errors: {2}",
                            r.getStatus(), r.getResourceId(), r.getErrors())
                    );
                }
                return r;
            })
            .collectList()
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Failed to delete user: {}", e.getResponseBodyAsString(), e);
                return Mono.error(e);
            })
            .then();
    }

    public Mono<List<User>> ingestUsers(List<User> users) {
        List<com.backbase.dbs.user.api.service.v2.model.User> userList = users.stream().map(mapper::toService).collect(Collectors.toList());
        return Mono.zip(Mono.just(users),
            usersApi.ingestUsers(userList)
                .onErrorContinue(WebClientResponseException.class, (throwable, o) -> {
                    log.error("Failed to bulk ingest users: {}", userList);
                })
                .collectList(), this::mergeBatchResults);


    }

    @NotNull
    private List<User> mergeBatchResults(List<User> users, List<BatchResponseItem> batchResponseItems) throws UserUpsertException {
        // current and batchResponseItems lists must be the same size;
        if (users.size() != batchResponseItems.size()) {
            throw new UserUpsertException("Batch Results response does not match request", users, batchResponseItems);
        } else {
            for (int i = 0; i < users.size(); i++) {

                User user = users.get(i);
                BatchResponseItem batchResponseItem = batchResponseItems.get(i);

                if (batchResponseItem.getErrors().isEmpty()) {
                    user.setInternalId(batchResponseItem.getResourceId());
                } else {
                    throw new UserUpsertException("Failed to upsert user", users, batchResponseItems);
                }
            }

            return users;
        }
    }

    /**
     * Create Realm.
     *
     * @param realmName
     * @return
     */
    private Mono<Realm> createRealm(final String realmName) {
        AddRealmRequest assignRealmRequest = new AddRealmRequest().realmName(realmName);
        return identityManagementApi.createRealm(assignRealmRequest)
            .doOnNext(addRealmResponse -> log.info("Realm Created: '{}'", addRealmResponse.getId()))
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Error creating realm: {} Response: {}", realmName, e.getResponseBodyAsString());
                return Mono.error(e);
            })
            .map(realmMapper::toStream);
    }

    /**
     * Checks for existing Realms and Returns if matching realm is found.
     *
     * @param realmName
     * @return
     */
    private Mono<Realm> existingRealm(final String realmName) {
        log.info("Checking for existing Realm '{}'", realmName);
        return identityManagementApi.getRealms(null)
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Error getting realm: {} Response: {}", realmName, e.getResponseBodyAsString());
                return Mono.error(e);
            })
            .filter(realm -> realmName.equals(realm.getRealmName()))
            .next();
    }

    /**
     * Setup realm checks if realm exists otherwise creates
     *
     * @param legalEntity
     * @return
     */
    public Mono<Realm> setupRealm(LegalEntity legalEntity) {
        if (StringUtils.isEmpty(legalEntity.getRealmName())) {
            return Mono.empty();
        }
        return existingRealm(legalEntity.getRealmName())
                .switchIfEmpty(createRealm(legalEntity.getRealmName()));
    }

    /**
     * Link LegalEntity to that Realm. (Realm should already be in DBS)
     *
     * @param legalEntity Legal entity object, contains the Realm Name and LE IDs
     * @return the same object on success
     */
    public Mono<LegalEntity> linkLegalEntityToRealm(LegalEntity legalEntity) {
        if (ObjectUtils.isEmpty(legalEntity.getRealmName())){
            log.warn("Skipping assigning legal entity to Identity, realm name not informed.");
            return Mono.empty();
        }
        log.info("Linking Legal Entity with internal Id '{}' to Realm: '{}'", legalEntity.getInternalId(), legalEntity.getRealmName());
        AssignRealm assignRealm = new AssignRealm().legalEntityId(legalEntity.getInternalId());
        return identityManagementApi.assignRealm(legalEntity.getRealmName(), assignRealm)
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Error Linking: {}", e.getResponseBodyAsString());
                return Mono.error(e);
            })
            .then(Mono.just(legalEntity))
            .map(actual -> {
                log.info("Legal Entity: {} linked to Realm: {}", actual.getInternalId(), legalEntity.getRealmName());
                return actual;
            });
    }

    /**
     * Create or Import User from Identity base on {@link IdentityUserLinkStrategy property}
     *
     * @param user
     * @param legalEntityInternalId
     * @return the same User with updated internal and external id on success
     */
    public Mono<User> createOrImportIdentityUser(User user, String legalEntityInternalId, StreamTask streamTask) {

        Mono<CreateIdentityResponse> upsertCall;

        if (IdentityUserLinkStrategy.CREATE_IN_IDENTITY.equals(user.getIdentityLinkStrategy())) {
            Objects.requireNonNull(user.getFullName(),
                "User Full Name is required for user: " + user.getExternalId() + " in legal entity: "
                    + legalEntityInternalId);
            Objects.requireNonNull(
                Optional.ofNullable(user.getEmailAddress()).map(EmailAddress::getAddress).orElse(null),
                "User Email Address is required for user: " + user.getExternalId() + " in legal entity: "
                    + legalEntityInternalId);
            Objects.requireNonNull(Optional.ofNullable(user.getMobileNumber()).map(PhoneNumber::getNumber).orElse(null),
                "User Mobile Number is required for user: " + user.getExternalId() + " in legal entity: "
                    + legalEntityInternalId);

            CreateIdentityRequest createIdentityRequest = new CreateIdentityRequest();
            createIdentityRequest.setLegalEntityInternalId(legalEntityInternalId);
            createIdentityRequest.setExternalId(user.getExternalId());
            createIdentityRequest.setAdditions(user.getAdditions());
            createIdentityRequest.setFullName(user.getFullName());
            createIdentityRequest.setEmailAddress(user.getEmailAddress().getAddress());
            createIdentityRequest.setMobileNumber(user.getMobileNumber().getNumber());
            ofNullable(user.getAttributes()).ifPresent(createIdentityRequest::setAttributes);

            upsertCall = identityManagementApi.createIdentity(createIdentityRequest)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error creating identity: {} Response: {}", createIdentityRequest,
                        e.getResponseBodyAsString());
                    String message = "Failed to create user: " + user.getExternalId();
                    streamTask.error("user", "create-identity", "failed",
                        user.getExternalId(), legalEntityInternalId, e, e.getMessage(), message);
                    return Mono.error(new StreamTaskException(streamTask, message));
                });
        } else {
            ImportIdentity importIdentity = new ImportIdentity();
            importIdentity.setLegalEntityInternalId(legalEntityInternalId);
            importIdentity.setExternalId(user.getExternalId());
            importIdentity.additions(user.getAdditions());
            upsertCall = identityManagementApi.importIdentity(importIdentity)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error importing identity: {} Response: {}", importIdentity, e.getResponseBodyAsString());
                    String message = "Failed to import user: " + user.getExternalId();
                    streamTask.error("user", "import-identity", "failed",
                        user.getExternalId(), legalEntityInternalId, e, e.getMessage(), message);
                    return Mono.error(new StreamTaskException(streamTask, message));
                });
        }

        return upsertCall
            .map(identityCreatedItem -> {
                user.setInternalId(identityCreatedItem.getInternalId());
                user.setExternalId(identityCreatedItem.getExternalId());
                return user;
            })
            .flatMap(newUser -> this.updateIdentityUser(user, streamTask));
    }

    private Mono<User> updateIdentityUser(User user, StreamTask streamTask) {
        if (IdentityUserLinkStrategy.IMPORT_FROM_IDENTIY.equals(user.getIdentityLinkStrategy())
            && (user.getAttributes() != null || user.getAdditions() != null)) {
            UpdateIdentityRequest replaceIdentity = new UpdateIdentityRequest();
            replaceIdentity.attributes(user.getAttributes());
            replaceIdentity.additions(user.getAdditions());
            return identityManagementApi.updateIdentity(user.getInternalId(), replaceIdentity)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error updating identity: {} Response: {}", user.getExternalId(), e.getResponseBodyAsString());
                    String message = "Failed to update identity: " + user.getExternalId();
                    streamTask.error("user", "update-identity-attributes-and-additions", "failed",
                        user.getExternalId(), user.getInternalId(), e, e.getMessage(), message);

                    return Mono.error(new StreamTaskException(streamTask, message));
                })
                .then(Mono.just(user));
        }
        return Mono.just(user);
    }

    /**
     * Locks/Unlocks the user is current status is different.
     *
     * @param user  user to be locked/unlocked.
     * @param realm user's realm.
     * @return user.
     */
    public Mono<User> updateUserState(User user, String realm) {
        log.info("Changing user {} state to locked status {}, additional realm roles {}, additional groups {}",
            user.getInternalId(), user.getLocked(), user.getAdditionalRealmRoles(), user.getAdditionalGroups());

        return identityIntegrationApi.map(api -> getIdentityUser(user, realm)
            .flatMap(currentUser -> {
                Optional<UserRequestBody> updateRequestBody = updateRequired(currentUser, user);

                if (updateRequestBody.isPresent()) {
                    return api.updateUserById(realm, user.getInternalId(), updateRequestBody.get());
                }

                return Mono.just(user);
            })
            .doOnNext(eur -> {
                log.info("User {} locked successfully", user.getExternalId());
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Error updated user {} state due to error: {}", user.getInternalId(), e.getResponseBodyAsString());
                return Mono.error(e);
            })
            .thenReturn(user)).orElse(Mono.just(user));
    }

    private Mono<EnhancedUserRepresentation> getIdentityUser(User user, String realm) {
        return identityIntegrationApi.map(api -> api.getUserById(realm, user.getInternalId())
                .doOnNext(eur -> {
                    log.info("Identity user found: {}", user.getExternalId());
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error retrieving identity user {}: {}", user.getInternalId(), e.getResponseBodyAsString());
                    return Mono.error(e);
                }))
            .orElse(Mono.empty());
    }

    private User handleCreateUserResult(User user, UserCreated userCreated) {
        log.info("Created user: {} with internalId: {}", user.getFullName(), userCreated.getId());
        user.setInternalId(userCreated.getId());
        return user;
    }

    private Optional<UserRequestBody> updateRequired(EnhancedUserRepresentation currentUser, User user) {
        boolean updateRequired = false;
        UserRequestBody userRequestBody = mapper.toPresentation(currentUser);

        if (currentUser.getEnabled() != null) {
            boolean shouldEnable = user.getLocked() == null ? currentUser.getEnabled() : !user.getLocked();

            if (!currentUser.getEnabled().equals(shouldEnable)) {
                updateRequired = true;

                userRequestBody.setEnabled(shouldEnable);

                log.debug("Enabled set to {}", shouldEnable);
            }
        }

        if (!CollectionUtils.isEmpty(user.getAdditionalRealmRoles())) {
            for (String realmRole : user.getAdditionalRealmRoles()) {
                if (userRequestBody.getRealmRoles().stream().noneMatch(realmRole::equals)) {
                    updateRequired = true;

                    userRequestBody.getRealmRoles().add(realmRole);

                    log.debug("Realm role {} added", realmRole);
                }
            }
        }

        if (!CollectionUtils.isEmpty(user.getAdditionalGroups())) {
            for (String group : user.getAdditionalGroups()) {
                if (userRequestBody.getGroups().stream().noneMatch(group::equals)) {
                    updateRequired = true;

                    userRequestBody.getGroups().add(group);

                    log.debug("Group {} added", group);
                }
            }
        }

        return updateRequired ? Optional.of(userRequestBody) : Optional.empty();
    }


    /**
     * Update Identity User, ex: emailAddress, mobileNumber, attributes, and additions
     *
     * @param user
     * @return Mono<User>
     */
    public Mono<User> updateIdentity(User user) {

        Objects.requireNonNull(user.getInternalId(), "user internalId is required");

        return identityManagementApi.getIdentity(user.getInternalId())
                .map(mapper::mapUpdateIdentity)
                .flatMap(updateIdentityRequest -> {
                    log.debug("Trying to update identity attributes and additions, externalId [{}]", user.getExternalId());
                        if (updateIdentityRequest.getAttributes() == null) {
                            updateIdentityRequest.attributes(new HashMap<>());
                        }
                        if (updateIdentityRequest.getAdditions() == null) {
                            updateIdentityRequest.additions(new HashMap<>());
                        }
                        updateIdentityRequest.getAttributes().putAll(requireNonNullElse(user.getAttributes(), Map.of()));
                        updateIdentityRequest.getAdditions().putAll(requireNonNullElse(user.getAdditions(), Map.of()));

                        return identityManagementApi.updateIdentity(user.getInternalId(), updateIdentityRequest)
                                .onErrorResume(WebClientResponseException.class, e -> {
                                    log.error("Failed to update identity: {}", e.getResponseBodyAsString(), e);
                                    return Mono.error(e);
                                });
                        }
                ).thenReturn(user);
    }

    /**
     * Update user
     *
     * @param user
     * @return Mono<Void>
     */
    public Mono<User> updateUser(User user) {
        return usersApi.updateUserInBatch(Collections.singletonList(new BatchUser()
                        .externalId(user.getExternalId())
                        .userUpdate(mapper.toServiceUser(user))))
                .map(r -> {
                    log.debug("Updated user response: status {} for resource {}, errors: {}", r.getStatus(), r.getResourceId(), r.getErrors());
                    if (r.getStatus().getValue() != null && !HttpStatus.valueOf(Integer.parseInt(r.getStatus().getValue())).is2xxSuccessful()) {
                        String errorMsg = MessageFormat.format(
                                "Failed item in the batch for User Update: status {0} for resource {1}, errors: {2}",
                                r.getStatus(), r.getResourceId(), r.getErrors());
                        throw new UserUpsertException(errorMsg, Collections.singletonList(user),
                                Collections.singletonList(r));
                    }
                    return r;
                })
                .collectList()
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Failed to update user: {}", e.getResponseBodyAsString(), e);
                    return Mono.error(e);
                })
                .then(getUserByExternalId(user.getExternalId()));
    }

}
