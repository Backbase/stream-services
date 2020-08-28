package com.backbase.stream.service;

import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.dbs.user.presentation.service.model.*;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.mapper.RealmMapper;
import com.backbase.stream.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stream User Management. Still needs to be adapted to use Identity correctly
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@AllArgsConstructor
public class UserService {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);
    private final RealmMapper realmMapper = Mappers.getMapper(RealmMapper.class);

    private final UsersApi usersApi;

    /**
     * Get User by extenral ID.
     *
     * @param externalId Extenral ID
     * @return User if exists. Empty if not.
     */
    public Mono<User> getUserByExternalId(String externalId) {
        return usersApi.getExternalIdByExternalId(externalId)
                .doOnNext(userItem -> log.info("Found user: {} for externalId: {}", userItem.getFullName(), userItem.getExternalId()))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                        handleUserNotFound(externalId, notFound.getResponseBodyAsString()))
                .map(mapper::toStream);
    }

    /**
     * Get Identity User.  WIP!
     *
     * @param externalId External ID
     * @return Identity User
     */
    public Mono<User> getIdentityUserByExternalId(String externalId) {
        return usersApi.getUserIdByUserId(externalId)
                .doOnNext(userItem -> log.info("Found user: {} for externalId: {}", userItem.getFullName(), userItem.getExternalId()))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                        handleUserNotFound(externalId, notFound.getResponseBodyAsString()))
                .map(mapper::toStream);
    }


    private Mono<? extends UserItem> handleUserNotFound(String externalId, String responseBodyAsString) {
        log.info("User with externalId: {} does not exist: {}", externalId, responseBodyAsString);
        return Mono.empty();
    }

    public Mono<User> createUser(User user, String legalEntityExternalId) {
        UserCreateItem userCreateItem = mapper.toPresentation(user);
        userCreateItem.setLegalEntityExternalId(legalEntityExternalId);
        return usersApi.postUsers(userCreateItem)
                .doOnError(WebClientResponseException.class, e -> handleCreateUserError(user, e))
                .map(idItem -> handleCreateUserResult(user, idItem));
    }

    /**
     * Get users for specified legal entity.
     *
     * @param legalEntityInternalId legal  entity internal id.
     * @return flux of user  items.
     */
    public Flux<SchemasUserItem> getUsersByLegalEntity(String legalEntityInternalId){
        log.debug("Retrieving users for Legal Entity '{}'", legalEntityInternalId);
        return usersApi.getUsers(legalEntityInternalId, null, null, null, null);
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
        return usersApi.putUsers(
                userExternalIds.stream()
                        .map(userExternalId -> new UserItemPut()
                                .externalId(userExternalId)
                                .userUpdate(new UserUpdate()
                                        .externalId("REMOVED_" + userExternalId + "_" + UUID.randomUUID().toString())
                                        .legalEntityId(legalEntityInternalId)
                                        .fullName("archived_" + userExternalId)))
                        .collect(Collectors.toList()))
                .map(r -> {
                    log.debug("Batch Archive User response: status {} for resource {}, errors: {}",r.getStatus(), r.getResourceId(), r.getErrors());
                    if (!r.getStatus().getValue().equals("200")) {
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

    /**
     * Create Realm.
     *
     * @param realmName
     * @return
     */
    private Mono<Realm> createRealm(final String realmName) {
        UsersIdentitiesRealmsPostResponse assignRealmRequest = new UsersIdentitiesRealmsPostResponse().realmName(realmName);
        return usersApi.postRealms(assignRealmRequest)
                .doOnNext(realm -> log.info("Realm Created: '{}'", realmName))
                .doOnError(WebClientResponseException.class, badRequest ->
                        log.error("Error creating Realm"))
                .map(realmMapper::toStream);
    }

    /**
     * Checks for existing Realms and Returns if matching realm is found.
     * @param realmName
     * @return
     */
    private Mono<Realm> existingRealm(final String realmName) {
        log.info("Checking for existing Realm '{}'", realmName);
        return usersApi.getRealms(null)
                .doOnError(WebClientResponseException.class, badRequest ->
                        log.error("Error getting Realms"))
                .collectList()
                .map(realms -> realms.stream().filter(realm -> realmName.equals(realm.getRealmName())).findFirst())
                .flatMap(Mono::justOrEmpty);
    }

    /**
     * Setup realm checks if realm exists otherwise creates
     * @param legalEntity
     * @return
     */
    public Mono<Realm> setupRealm(LegalEntity legalEntity){
        if(StringUtils.isEmpty(legalEntity.getRealmName())){
            return Mono.empty();
        }
        Mono<Realm> existingRealm = existingRealm(legalEntity.getRealmName());
        Mono<Realm> createNewRealm = createRealm(legalEntity.getRealmName());
        return existingRealm.switchIfEmpty(createNewRealm)
                .map(actual -> actual);

    }

    /**
     * Link LegalEntity to that Realm. (Realm should already be in DBS)
     *
     * @param legalEntity Legal entity object, contains the Realm Name and LE IDs
     * @return the same object on success
     */
    public Mono<LegalEntity> linkLegalEntityToRealm(LegalEntity legalEntity) {
        log.info("Linking Legal Entity with internal Id '{}' to Realm: '{}'", legalEntity.getInternalId(), legalEntity.getRealmName());
        AssignRealmResponse assignRealmRequest = new AssignRealmResponse().legalEntityId(legalEntity.getInternalId());
        return usersApi.postLegalentitiesByRealmName(legalEntity.getRealmName(), assignRealmRequest)
                .doOnError(WebClientResponseException.BadRequest.class, badRequest ->
                        log.error("Error Linking: {}", badRequest.getResponseBodyAsString()))
                .then(Mono.just(legalEntity))
                .map(actual -> {
                    log.info("Legal Entity: {} linked to Realm: {}", actual.getInternalId(), legalEntity.getRealmName());
                    return actual;
                });
    }

    /**
     * Create or Import User from Identity base on {@link IdentityUserLinkStrategy property}
     * @param user
     * @param legalEntityInternalId
     * @return the same User with updated internal and external id on success
     */
    public Mono<User> createOrImportIdentityUser(User user, String legalEntityInternalId) {

        IdentityImportItem identityImportItem = new IdentityImportItem();
        identityImportItem.setLegalEntityInternalId(legalEntityInternalId);
        identityImportItem.setExternalId(user.getExternalId());

        if (IdentityUserLinkStrategy.CREATE_IN_IDENTITY.equals(user.getIdentityLinkStrategy())) {
            Objects.requireNonNull(user.getFullName(), "User Full Name is required");
            Objects.requireNonNull(user.getEmailAddress(), "User Email Address is required");
            Objects.requireNonNull(user.getMobileNumber(), "User Mobile Number is required");

            identityImportItem.setFullName(user.getFullName());
            identityImportItem.setEmailAddress(user.getEmailAddress().getAddress());
            identityImportItem.setMobileNumber(user.getMobileNumber().getNumber());
        }

        return usersApi.postIdentities(identityImportItem)
                .map(identityCreatedItem -> {
                    user.setInternalId(identityCreatedItem.getInternalId());
                    user.setExternalId(identityCreatedItem.getExternalId());
                    return user;
                });
    }


    /**
     * Update identity user attributes
     * @param user
     * @return {@link Mono<Void>}
     */
    public Mono<Void> updateIdentityUserAttributes(User user) {

        IdentityPutItem identityPutItem = new IdentityPutItem();
        identityPutItem.attributes(user.getAttributes());

        return usersApi.putInternalIdByInternalId(user.getInternalId(), identityPutItem);
    }

    private User handleCreateUserResult(User user, IdItem idItem) {
        log.info("Created user: {} with internalId: {}", user.getFullName(), idItem.getId());
        user.setInternalId(idItem.getId());
        return user;
    }

    private void handleCreateUserError(User user, WebClientResponseException response) {
        log.error("Created user: {} with internalId: {}", user, response.getResponseBodyAsString());
    }
}
