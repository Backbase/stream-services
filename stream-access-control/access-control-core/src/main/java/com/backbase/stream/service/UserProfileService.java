package com.backbase.stream.service;

import com.backbase.dbs.userprofile.api.UserProfileApi;
import com.backbase.dbs.userprofile.model.CreateUserProfile;
import com.backbase.dbs.userprofile.model.GetUserProfile;
import com.backbase.dbs.userprofile.model.ReplaceUserProfile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Stream User Profile Management
 */
@Slf4j
@AllArgsConstructor
public class UserProfileService {

    private final UserProfileApi userProfileApi;

    public Mono<GetUserProfile> upsertUserProfile(CreateUserProfile requestBody) {
        Mono<GetUserProfile> getExistingUser = getUserProfileByUserID(requestBody.getUserId());
        return getExistingUser != null ?
            getExistingUser : createUserProfile(requestBody);
    }

    public Mono<GetUserProfile> createUserProfile(CreateUserProfile requestBody) {
        return userProfileApi.postCreateUserProfile(requestBody)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to create User Profile: {}\n{}", requestBody.getExternalId(),
                    throwable.getResponseBodyAsString()));
    }

    public Mono<GetUserProfile> updateUserProfile(String userId, ReplaceUserProfile requestBody) {
        return userProfileApi.putReplaceUserProfileByUserID(userId, requestBody)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to create User Profile: {}\n{}", requestBody.getExternalId(),
                    throwable.getResponseBodyAsString()));
    }

    public Mono<Void> deleteUserProfile(String userId) {
        return userProfileApi.deleteDeleteUserProfileByUserID(userId)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to delete User Profile: {}\n{}", userId, throwable.getResponseBodyAsString()));
    }

    public Mono<GetUserProfile> getUserProfileByUserID(String userId) {
        if (userId == null) {
            return Mono.empty();
        }
        return userProfileApi.getGetUserProfileByUserID(userId)
            .doOnNext(
                userProfileItem -> log.info("Found User Profile for externalId: {}", userProfileItem.getExternalId()))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                handleUserNotFound(userId, notFound.getResponseBodyAsString()));
    }

    private Mono<? extends GetUserProfile> handleUserNotFound(String externalId, String responseBodyAsString) {
        log.info("User with externalId: {} does not exist: {}", externalId, responseBodyAsString);
        return Mono.empty();
    }

}
